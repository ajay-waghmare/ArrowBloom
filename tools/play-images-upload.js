/* Arrow Bloom — upload the store listing graphics (icon, feature graphic, phone screenshots) for ONE
 * language in ONE edit + ONE commit via the Play Developer API (edits.images.*).
 * Usage: node tools/play-images-upload.js <sa.json> <package> [lang=en-US] [--dry-run] [--list]
 *   --list     open an edit, print the images currently on that listing, discard the edit (read-only)
 *   --dry-run  deleteall + upload inside an edit, read the counts back, then DELETE the edit (nothing published)
 * Files, relative to the repo root: store/icon-512.png, store/feature-1024x500.png, store/screenshots/NN-*.png (sorted).
 * Languages without their own graphics fall back to the default language's graphics on Play.
 * Fails CLOSED: every file is checked (PNG, size, aspect, count) before the edit is opened.
 * Node 16 compatible: built-in https + crypto only. */
const fs=require('fs'),path=require('path'),crypto=require('crypto'),https=require('https');
const pos=process.argv.slice(2).filter(a=>!a.startsWith('--')),flags=new Set(process.argv.slice(2).filter(a=>a.startsWith('--')));
const [KEY,PKG,LANG='en-US']=pos;const DRY=flags.has('--dry-run'),LIST=flags.has('--list');
if(!KEY||!PKG){console.error('usage: <sa.json> <package> [lang] [--dry-run] [--list]');process.exit(2);}
const ROOT=path.resolve(__dirname,'..'),SHOTS=path.join(ROOT,'store/screenshots');
const PLAN=[
 {type:'icon',files:[path.join(ROOT,'store/icon-512.png')]},
 {type:'featureGraphic',files:[path.join(ROOT,'store/feature-1024x500.png')]},
 {type:'phoneScreenshots',files:fs.existsSync(SHOTS)?fs.readdirSync(SHOTS).filter(f=>/^\d\d-.*\.png$/.test(f)).sort().map(f=>path.join(SHOTS,f)):[]},
];
const sa=JSON.parse(fs.readFileSync(KEY,'utf8'));
const b64=s=>Buffer.from(s).toString('base64').replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
const req=(o,b)=>new Promise((res,rej)=>{const r=https.request(o,x=>{let d=[];x.on('data',c=>d.push(c));x.on('end',()=>res({status:x.statusCode,body:Buffer.concat(d).toString('utf8')}))});r.on('error',rej);if(b)r.write(b);r.end()});
async function token(){const now=Math.floor(Date.now()/1000);
 const si=`${b64(JSON.stringify({alg:'RS256',typ:'JWT'}))}.${b64(JSON.stringify({iss:sa.client_email,scope:'https://www.googleapis.com/auth/androidpublisher',aud:sa.token_uri,iat:now,exp:now+3600}))}`;
 const sg=crypto.createSign('RSA-SHA256');sg.update(si);
 const form=`grant_type=${encodeURIComponent('urn:ietf:params:oauth:grant-type:jwt-bearer')}&assertion=${si}.${b64(sg.sign(sa.private_key))}`;
 const r=await req({method:'POST',hostname:'oauth2.googleapis.com',path:'/token',headers:{'Content-Type':'application/x-www-form-urlencoded','Content-Length':Buffer.byteLength(form)}},form);
 const j=JSON.parse(r.body);if(!j.access_token)throw new Error('auth: '+r.body.slice(0,200));return j.access_token;}
const H='androidpublisher.googleapis.com';
function png(file){const b=fs.readFileSync(file);if(b.readUInt32BE(0)!==0x89504e47)throw new Error('not a PNG: '+file);return{b,w:b.readUInt32BE(16),h:b.readUInt32BE(20)};}
function validate(){let bad=false;
 for(const p of PLAN){for(const f of p.files){
  if(!fs.existsSync(f)){console.log(`✗ missing ${f}`);bad=true;continue;}
  const{b,w,h}=png(f);let ok=b.length<=8*1024*1024;
  if(p.type==='icon')ok=ok&&w===512&&h===512;
  if(p.type==='featureGraphic')ok=ok&&w===1024&&h===500;
  if(p.type==='phoneScreenshots')ok=ok&&w>=320&&h>=320&&w<=3840&&h<=3840&&Math.max(w,h)/Math.min(w,h)<=2;
  console.log(`${ok?'✓':'✗'} ${p.type.padEnd(16)} ${path.basename(f).padEnd(24)} ${w}x${h} ${Math.round(b.length/1024)} KB`);if(!ok)bad=true;}}
 const n=PLAN[2].files.length;if(n<2||n>8){console.log(`✗ phoneScreenshots count ${n} (Play needs 2–8)`);bad=true;}
 return!bad;}
(async()=>{
 if(!LIST&&!validate()){console.error('\nVALIDATION FAILED — nothing sent.');process.exit(3);}
 const tk=await token();const A={Authorization:`Bearer ${tk}`};
 const base=`/androidpublisher/v3/applications/${PKG}/edits`;
 const mk=await req({method:'POST',hostname:H,path:base,headers:{...A,'Content-Length':0}});
 if(mk.status!==200)throw new Error('edits.insert '+mk.status+' '+mk.body.slice(0,300));
 const id=JSON.parse(mk.body).id;console.log(`edit ${id} opened${LIST?' (LIST — read-only, will be discarded)':DRY?' (DRY RUN — will be discarded)':''}`);
 const list=async t=>{const g=await req({method:'GET',hostname:H,path:`${base}/${id}/listings/${LANG}/${t}`,headers:A});if(g.status!==200)return{err:g.status+' '+g.body.slice(0,120)};return{images:(JSON.parse(g.body||'{}').images||[])};};
 let fail=false;
 if(LIST){for(const p of PLAN){const r=await list(p.type);console.log(`${p.type.padEnd(16)} ${r.err?'HTTP '+r.err:r.images.length+' image(s)'+(r.images.length?': '+r.images.map(i=>i.id).join(', '):'')}`);}}
 else{for(const p of PLAN){
  const del=await req({method:'DELETE',hostname:H,path:`${base}/${id}/listings/${LANG}/${p.type}`,headers:A});
  console.log(`${p.type}: deleteall → HTTP ${del.status}`);if(del.status!==200){fail=true;continue;}
  for(const f of p.files){const{b}=png(f);let up,tries=0;
   for(;;){up=await req({method:'POST',hostname:H,path:`/upload/androidpublisher/v3/applications/${PKG}/edits/${id}/listings/${LANG}/${p.type}?uploadType=media`,headers:{...A,'Content-Type':'image/png','Content-Length':b.length}},b);if(up.status<500||++tries>=4)break;await new Promise(r=>setTimeout(r,800*tries));}
   console.log(`  upload ${path.basename(f).padEnd(24)} → HTTP ${up.status}${up.status!==200?'  '+up.body.slice(0,200):''}`);if(up.status!==200)fail=true;}
  const r=await list(p.type);const n=r.images?r.images.length:-1;const ok=n===p.files.length;console.log(`  read-back ${n}/${p.files.length} ${ok?'✓':'✗'}`);if(!ok)fail=true;}}
 if(LIST||DRY||fail){const d=await req({method:'DELETE',hostname:H,path:`${base}/${id}`,headers:A});console.log(`edit discarded (HTTP ${d.status}). Nothing published.${fail?' (FAILURES above)':''}`);process.exit(fail?5:0);}
 const cm=await req({method:'POST',hostname:H,path:`${base}/${id}:commit`,headers:{...A,'Content-Length':0}});
 console.log(`edits.commit → HTTP ${cm.status}`);if(cm.status!==200){console.log(cm.body.slice(0,600));process.exit(6);}
 console.log(`\n✔ graphics for ${LANG} COMMITTED in one edit.`);
})().catch(e=>{console.error('FATAL',e.message);process.exit(1)});
