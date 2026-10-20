/*
 * Arrow Bloom — upload a signed AAB and stage it as a DRAFT on a track.
 *
 * Flow: auth -> edits.insert -> upload bundle -> set track release (status=draft)
 *       -> edits.commit  (Google sends the changes for review automatically;
 *          this app REJECTS changesNotSentForReview=true, so we must not set it).
 * It stages a DRAFT only — nothing is rolled out / made live to testers.
 *
 * Usage:
 *   node tools/play-upload.js <aabPath> [packageName] [track] [releaseName]
 *   credential via PLAY_SA_JSON env var (in-memory; never written to disk).
 *
 * Node 16 compatible: built-in https + crypto only.
 */
const fs = require('fs');
const crypto = require('crypto');
const https = require('https');

const AAB_PATH = process.argv[2];
const PACKAGE = process.argv[3] || 'com.aweagle.arrowbloom';
const TRACK = process.argv[4] || 'internal';
const RELEASE_NAME = process.argv[5] || '1.0.0 (1)';

if (!AAB_PATH || !fs.existsSync(AAB_PATH)) {
  console.error(`AAB not found: ${AAB_PATH}`);
  process.exit(2);
}

function loadCredential() {
  // Preferred: point PLAY_SA_FILE at the service-account JSON you downloaded
  // (the key never has to be pasted anywhere).
  if (process.env.PLAY_SA_FILE && process.env.PLAY_SA_FILE.trim()) {
    return JSON.parse(fs.readFileSync(process.env.PLAY_SA_FILE.trim(), 'utf8'));
  }
  // Or pass the JSON inline via PLAY_SA_JSON.
  if (process.env.PLAY_SA_JSON && process.env.PLAY_SA_JSON.trim()) {
    return JSON.parse(process.env.PLAY_SA_JSON);
  }
  throw new Error('Set PLAY_SA_FILE (path to the JSON file) or PLAY_SA_JSON (inline JSON) env var.');
}
const sa = loadCredential();

function b64url(input) {
  return Buffer.from(input).toString('base64')
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
function httpRequest(options, body) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => resolve({ status: res.statusCode, body: data }));
    });
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}
function safeParse(s) { try { return JSON.parse(s); } catch (e) { return null; } }

async function getAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const claims = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: sa.token_uri, iat: now, exp: now + 3600,
  }));
  const signingInput = `${header}.${claims}`;
  const signer = crypto.createSign('RSA-SHA256');
  signer.update(signingInput);
  const jwt = `${signingInput}.${b64url(signer.sign(sa.private_key))}`;
  const form = `grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${jwt}`;
  const res = await httpRequest({
    method: 'POST', hostname: 'oauth2.googleapis.com', path: '/token',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Content-Length': Buffer.byteLength(form) },
  }, form);
  const json = safeParse(res.body);
  if (res.status !== 200 || !json || !json.access_token) throw new Error(`Auth failed (${res.status}): ${res.body}`);
  return json.access_token;
}

function api(token, method, path, body) {
  const headers = { Authorization: `Bearer ${token}` };
  let payload = null;
  if (body) {
    payload = JSON.stringify(body);
    headers['Content-Type'] = 'application/json';
    headers['Content-Length'] = Buffer.byteLength(payload);
  }
  return httpRequest({
    method, hostname: 'androidpublisher.googleapis.com',
    path: `/androidpublisher/v3/applications/${encodeURIComponent(PACKAGE)}${path}`, headers,
  }, payload).then((r) => ({ status: r.status, json: safeParse(r.body), raw: r.body }));
}

function uploadBundle(token, editId, buf) {
  return httpRequest({
    method: 'POST', hostname: 'androidpublisher.googleapis.com',
    path: `/upload/androidpublisher/v3/applications/${encodeURIComponent(PACKAGE)}/edits/${editId}/bundles?uploadType=media`,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/octet-stream', 'Content-Length': buf.length },
  }, buf).then((r) => ({ status: r.status, json: safeParse(r.body), raw: r.body }));
}

function die(msg, detail) { console.error(`\n✖ ${msg}`); if (detail) console.error(detail); process.exit(3); }

(async () => {
  console.log('Uploading clean no-ads build as a DRAFT (nothing goes live)…');
  console.log(`  package : ${PACKAGE}`);
  console.log(`  track   : ${TRACK}`);
  console.log(`  aab     : ${AAB_PATH}\n`);

  const token = await getAccessToken();
  console.log('• auth OK');

  const edit = await api(token, 'POST', '/edits');
  if (edit.status !== 200 || !edit.json) die(`edits.insert failed (${edit.status})`, edit.raw);
  const editId = edit.json.id;
  console.log(`• edit opened (${editId})`);

  const buf = fs.readFileSync(AAB_PATH);
  const up = await uploadBundle(token, editId, buf);
  if (up.status !== 200 || !up.json || up.json.versionCode == null) {
    // discard edit before failing
    await api(token, 'DELETE', `/edits/${editId}`);
    die(`bundle upload failed (${up.status})`, up.raw);
  }
  const vc = up.json.versionCode;
  console.log(`• bundle uploaded → versionCode ${vc} (sha256 ${(up.json.sha256 || '').slice(0, 16)}…)`);

  const trackBody = { track: TRACK, releases: [{ name: RELEASE_NAME, status: 'draft', versionCodes: [String(vc)] }] };
  const tr = await api(token, 'PUT', `/edits/${editId}/tracks/${encodeURIComponent(TRACK)}`, trackBody);
  if (tr.status !== 200) {
    await api(token, 'DELETE', `/edits/${editId}`);
    die(`track update failed (${tr.status})`, tr.raw);
  }
  console.log(`• track '${TRACK}' set → draft release "${RELEASE_NAME}" [${vc}]`);

  // No changesNotSentForReview param: this app forces review, so Google sends the
  // changes for review automatically. Setting the param returns HTTP 400.
  const commit = await api(token, 'POST', `/edits/${editId}:commit`);
  if (commit.status !== 200) {
    die(`commit failed (${commit.status})`, commit.raw + '\n(The edit was NOT committed; nothing changed.)');
  }
  console.log(`• committed (edit ${editId}).`);
  console.log(`\n✔ Done. versionCode ${vc} is now the DRAFT on the '${TRACK}' track. Not yet rolled out.`);
})().catch((e) => { die('unexpected error', e && e.stack ? e.stack : String(e)); });
