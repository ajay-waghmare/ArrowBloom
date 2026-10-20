/*
 * Arrow Bloom — read-only Play Console status check.
 *
 * Authenticates a Google service account (JWT -> OAuth2) and queries the
 * Android Publisher API. It opens an EDIT, reads tracks/bundles/details, then
 * DELETES the edit without committing — so it changes nothing in the Console.
 *
 * Usage:
 *   node tools/play-status.js [serviceAccountJson] [packageName]
 *   defaults: play-account.json   com.aweagle.arrowbloom
 *
 * Node 16 compatible: uses built-in https + crypto only (no deps, no fetch).
 */
const fs = require('fs');
const crypto = require('crypto');
const https = require('https');

const PACKAGE = process.argv[3] || 'com.aweagle.arrowbloom';

// Credential source, in priority order:
//   1. PLAY_SA_JSON env var  (in-memory, never written to disk — preferred)
//   2. a file path passed as argv[2]
//   3. JSON piped via stdin
function loadCredential() {
  if (process.env.PLAY_SA_JSON && process.env.PLAY_SA_JSON.trim()) {
    return JSON.parse(process.env.PLAY_SA_JSON);
  }
  const keyPath = process.argv[2];
  if (keyPath && fs.existsSync(keyPath)) {
    return JSON.parse(fs.readFileSync(keyPath, 'utf8'));
  }
  try {
    const piped = fs.readFileSync(0, 'utf8');
    if (piped && piped.trim()) return JSON.parse(piped);
  } catch (e) { /* no stdin */ }
  throw new Error('No credential. Set PLAY_SA_JSON env var, pass a JSON file path, or pipe JSON via stdin.');
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

function safeParse(s) {
  try { return JSON.parse(s); } catch (e) { return null; }
}

async function getAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const claims = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: sa.token_uri,
    iat: now,
    exp: now + 3600,
  }));
  const signingInput = `${header}.${claims}`;
  const signer = crypto.createSign('RSA-SHA256');
  signer.update(signingInput);
  const signature = b64url(signer.sign(sa.private_key));
  const jwt = `${signingInput}.${signature}`;

  const form = `grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${jwt}`;
  const res = await httpRequest({
    method: 'POST',
    hostname: 'oauth2.googleapis.com',
    path: '/token',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': Buffer.byteLength(form),
    },
  }, form);
  const json = safeParse(res.body);
  if (res.status !== 200 || !json || !json.access_token) {
    throw new Error(`Auth failed (${res.status}): ${res.body}`);
  }
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
    method,
    hostname: 'androidpublisher.googleapis.com',
    path: `/androidpublisher/v3/applications/${encodeURIComponent(PACKAGE)}${path}`,
    headers,
  }, payload).then((r) => ({ status: r.status, json: safeParse(r.body), raw: r.body }));
}

function line(c) { return c.repeat(64); }

(async () => {
  console.log(line('='));
  console.log('Arrow Bloom — Play Console status (read-only)');
  console.log(`Package : ${PACKAGE}`);
  console.log(`Service : ${sa.client_email}`);
  console.log(`Project : ${sa.project_id}`);
  console.log(line('='));

  let token;
  try {
    token = await getAccessToken();
    console.log('OAuth   : OK (token acquired)\n');
  } catch (e) {
    console.error('OAuth   : FAILED');
    console.error(String(e.message || e));
    process.exit(2);
  }

  const edit = await api(token, 'POST', '/edits');
  if (edit.status !== 200 || !edit.json || !edit.json.id) {
    console.error(`edits.insert FAILED (HTTP ${edit.status}).`);
    console.error(edit.raw);
    console.error('\nInterpretation:');
    if (edit.status === 401) console.error(' - 401: credential/signing problem.');
    if (edit.status === 403) console.error(' - 403: service account not granted access to this app, OR Android Publisher API not enabled on the project.');
    if (edit.status === 404) console.error(' - 404: package not found — app may not exist in this Play Console account, or SA not linked to it.');
    process.exit(3);
  }
  const editId = edit.json.id;
  console.log(`Edit    : opened (${editId}) — will be discarded, nothing is committed.\n`);

  try {
    const [tracks, bundles, details] = await Promise.all([
      api(token, 'GET', `/edits/${editId}/tracks`),
      api(token, 'GET', `/edits/${editId}/bundles`),
      api(token, 'GET', `/edits/${editId}/details`),
    ]);

    console.log(line('-'));
    console.log('APP DETAILS');
    console.log(line('-'));
    if (details.status === 200 && details.json) {
      console.log(`  defaultLanguage : ${details.json.defaultLanguage || '(none)'}`);
      console.log(`  contactEmail    : ${details.json.contactEmail || '(none)'}`);
      console.log(`  contactWebsite  : ${details.json.contactWebsite || '(none)'}`);
    } else {
      console.log(`  (details unavailable: HTTP ${details.status}) ${details.raw}`);
    }

    console.log('\n' + line('-'));
    console.log('UPLOADED BUNDLES (AABs Google has received)');
    console.log(line('-'));
    if (bundles.status === 200 && bundles.json && Array.isArray(bundles.json.bundles)) {
      if (bundles.json.bundles.length === 0) {
        console.log('  NONE — no AAB has ever been uploaded to this app.');
      } else {
        bundles.json.bundles.forEach((b) => {
          console.log(`  versionCode ${b.versionCode}   sha256=${(b.sha256 || '').slice(0, 16)}…`);
        });
      }
    } else {
      console.log(`  (bundles unavailable: HTTP ${bundles.status}) ${bundles.raw}`);
    }

    console.log('\n' + line('-'));
    console.log('TRACKS & RELEASES');
    console.log(line('-'));
    if (tracks.status === 200 && tracks.json && Array.isArray(tracks.json.tracks)) {
      if (tracks.json.tracks.length === 0) {
        console.log('  No tracks have any releases yet.');
      }
      tracks.json.tracks.forEach((t) => {
        console.log(`\n  ▸ track: ${t.track}`);
        if (!t.releases || t.releases.length === 0) {
          console.log('      (no releases)');
          return;
        }
        t.releases.forEach((r) => {
          console.log(`      - name        : ${r.name || '(unnamed)'}`);
          console.log(`        status      : ${r.status}`);
          console.log(`        versionCodes: ${(r.versionCodes || []).join(', ') || '(none)'}`);
          if (r.userFraction != null) console.log(`        userFraction: ${r.userFraction}`);
          if (r.releaseNotes) console.log(`        releaseNotes: ${r.releaseNotes.length} locale(s)`);
        });
      });
    } else {
      console.log(`  (tracks unavailable: HTTP ${tracks.status}) ${tracks.raw}`);
    }

    console.log('\n' + line('='));
    console.log('VERDICT (release/track side — App content declarations are NOT in this API)');
    console.log(line('='));
    const hasBundle = bundles.json && Array.isArray(bundles.json.bundles) && bundles.json.bundles.length > 0;
    const anyRelease = tracks.json && Array.isArray(tracks.json.tracks) &&
      tracks.json.tracks.some((t) => (t.releases || []).some((r) => (r.versionCodes || []).length > 0));
    console.log(`  AAB uploaded to Google : ${hasBundle ? 'YES' : 'NO'}`);
    console.log(`  On a testing/prod track: ${anyRelease ? 'YES' : 'NO'}`);
    if (!hasBundle) console.log('  => First upload still pending. The first AAB must be uploaded via the Console UI.');
    else if (!anyRelease) console.log('  => Bundle exists but is not assigned to any track release yet.');
    else console.log('  => A release exists on a track. Check the status field above (draft / inProgress / completed / halted).');
  } finally {
    const del = await api(token, 'DELETE', `/edits/${editId}`);
    console.log(`\nEdit    : discarded (HTTP ${del.status}). No changes were committed.`);
  }
})().catch((e) => {
  console.error('UNEXPECTED ERROR:', e && e.stack ? e.stack : e);
  process.exit(1);
});
