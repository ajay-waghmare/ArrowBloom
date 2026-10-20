/* Create/update SEVERAL localized listings in ONE edit + ONE commit.
 * Usage: node play-listings-set-multi.js <sa.json> <package> <bundle.json> [--dry-run]
 * bundle.json = {"pt-BR":{"title":..,"shortDescription":..,"fullDescription":..}, "es-419":{...}, ...}
 * Fails CLOSED: validates every locale (limits, promo terms in title) BEFORE opening the edit.
 * --dry-run: PUT all + read back inside an edit, then DELETE the edit (nothing published).
 * Images untouched — Play falls back to default-language graphics. Verifies each PUT by GET inside the edit. */
const fs=require('fs'),crypto=require('crypto'),https=require('https');
const [KEY,PKG,FILE]=process.argv.slice(2); const DRY=process.argv.includes('--dry-run');
if(!KEY||!PKG||!FILE){console.error('usage: <sa.json> <package> <bundle.json> [--dry-run]');process.exit(2);}
const sa=JSON.parse(fs.readFileSync(KEY,'utf8')); const B=JSON.parse(fs.readFileSync(FILE,'utf8'));
const len=s=>[...(s||'')].length; const LIM={title:30,shortDescription:80,fullDescription:4000};
const PROMO=/\b(free|best|top|#1|no\.?\s?1|grátis|gratis|gratuito|gratuit|melhor|mejor|sin anuncios|sem anúncios|tanpa iklan|terbaik|безкоштовно|найкращ|без реклами|無料|最高|広告なし|مجاني|مجانية|بدون إعلانات|الأفضل|ücretsiz|reklamsız|en iyi|免費|無廣告|最佳|miễn phí|không quảng cáo|tốt nhất)\b/i;
let bad=false;
for(const [lang,L] of Object.entries(B)){ console.log(`── ${lang}`);
 for(const k of Object.keys(LIM)){const n=len(L[k]);const ok=n>0&&n<=LIM[k];console.log(`   ${ok?'✓':'✗'} ${k.padEnd(16)} ${String(n).padStart(4)}/${LIM[k]}`);if(!ok)bad=true;}
 if(PROMO.test(L.title)){console.log(`   ✗ TITLE has a promotional term ("${L.title}")`);bad=true;}
 if(!/Arrow Bloom/.test(L.title)){console.log(`   ✗ TITLE missing Latin brand "Arrow Bloom"`);bad=true;}
}
if(bad){console.error('\nVALIDATION FAILED — nothing sent.');process.exit(3);}
const b64=s=>Buffer.from(s).toString('base64').replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
const req=(o,b)=>new Promise((res,rej)=>{const r=https.request(o,x=>{let d=[];x.on('data',c=>d.push(c));x.on('end',()=>res({status:x.statusCode,body:Buffer.concat(d).toString('utf8')}))});r.on('error',rej);if(b)r.write(b);r.end()});
async function token(){const now=Math.floor(Date.now()/1000);
 const si=`${b64(JSON.stringify({alg:'RS256',typ:'JWT'}))}.${b64(JSON.stringify({iss:sa.client_email,scope:'https://www.googleapis.com/auth/androidpublisher',aud:sa.token_uri,iat:now,exp:now+3600}))}`;
 const sg=crypto.createSign('RSA-SHA256');sg.update(si);
 const form=`grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${si}.${b64(sg.sign(sa.private_key))}`;
 const r=await req({method:'POST',hostname:'oauth2.googleapis.com',path:'/token',headers:{'Content-Type':'application/x-www-form-urlencoded','Content-Length':Buffer.byteLength(form)}},form);
 const j=JSON.parse(r.body);if(!j.access_token)throw new Error('auth: '+r.body.slice(0,200));return j.access_token;}
const H='androidpublisher.googleapis.com';
(async()=>{
 const tk=await token();const A={Authorization:`Bearer ${tk}`,'Content-Type':'application/json'};
 const base=`/androidpublisher/v3/applications/${PKG}/edits`;
 const mk=await req({method:'POST',hostname:H,path:base,headers:{...A,'Content-Length':0}});
 if(mk.status!==200)throw new Error('edits.insert '+mk.status+' '+mk.body.slice(0,300));
 const id=JSON.parse(mk.body).id; console.log(`\nedit ${id} opened${DRY?' (DRY RUN — will be discarded)':''}`);
 let fail=false;
 for(const [lang,L] of Object.entries(B)){
  const body=JSON.stringify({language:lang,title:L.title,shortDescription:L.shortDescription,fullDescription:L.fullDescription});
  // Google returns a random 502 on these PUTs every few runs. Without a retry a single
  // transient error would leave one locale out of the edit, and the commit would then
  // publish a PARTIAL set - worse than failing outright.
  let put, tries=0;
  for(;;){
    put=await req({method:'PUT',hostname:H,path:`${base}/${id}/listings/${lang}`,headers:{...A,'Content-Length':Buffer.byteLength(body)}},body);
    if(put.status<500 || ++tries>=4) break;
    await new Promise(r=>setTimeout(r,800*tries));
  }
  let verified='';
  if(put.status===200){const g=await req({method:'GET',hostname:H,path:`${base}/${id}/listings/${lang}`,headers:A});const x=JSON.parse(g.body);verified=(x.title===L.title&&x.shortDescription===L.shortDescription&&x.fullDescription===L.fullDescription)?'  read-back identical ✓':'  read-back DIFFERS ✗';if(!verified.includes('✓'))fail=true;} else fail=true;
  console.log(`listings.update ${lang.padEnd(6)} → HTTP ${put.status}${verified}${put.status!==200?'  '+put.body.slice(0,200):''}`);
 }
 const rb=await req({method:'GET',hostname:H,path:`${base}/${id}/listings`,headers:A});
 console.log('locales in edit:',(JSON.parse(rb.body).listings||[]).map(x=>x.language).join(', '));
 if(DRY||fail){const d=await req({method:'DELETE',hostname:H,path:`${base}/${id}`,headers:A});console.log(`edit discarded (HTTP ${d.status}). Nothing published.${fail?' (FAILURES above)':''}`);process.exit(fail?5:0);}
 const cm=await req({method:'POST',hostname:H,path:`${base}/${id}:commit`,headers:{...A,'Content-Length':0}});
 console.log(`edits.commit → HTTP ${cm.status}`); if(cm.status!==200){console.log(cm.body.slice(0,600));process.exit(6);}
 console.log(`\n✔ ${Object.keys(B).length} listings COMMITTED in one edit (goes through Google review).`);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1)});
