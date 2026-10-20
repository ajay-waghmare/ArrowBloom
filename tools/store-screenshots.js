// Frames the raw emulator captures in store/screenshots/src/*.png (1080x2400) as 1080x1920 (9:16) Play
// phone screenshots with a caption, rendered by headless Chrome. Play rejects images taller than 2:1,
// so raw 1080x2400 captures cannot be uploaded as they are.
//   node tools/store-screenshots.js            (needs Chrome at the path below, or set CHROME=...)
// Every NN-*.png in store/screenshots/ is rebuilt; old framed files without a source are removed.
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const DIR = path.resolve(__dirname, '../store/screenshots').replace(/\\/g, '/');
const CHROME = process.env.CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
// name, caption, sub-caption, accent (the garden colour of that screen), background tint
const shots = [
  ['01-board', 'Tap. Slide. Bloom.', 'Every arrow leaves along its own line', '#3049D8', '#E9EDF8'],
  ['02-hint', 'Stuck? A hint lights the way', 'It shows an arrow that can leave now', '#0B7C75', '#E0F4F1'],
  ['03-shapes', 'Boards that bloom into shapes', 'Hearts, flowers, diamonds and circles', '#237A4B', '#E6F4E9'],
  ['04-hearts', 'Three hearts per board', 'A blocked arrow bumps back. No timer, ever', '#E5484D', '#E6F4E9'],
  ['05-daily', 'One Daily Bloom for everyone', 'Keep your streak, share a spoiler-free result', '#C23468', '#FCE6EF'],
  ['06-home', 'Five gardens, then Infinite Bloom', 'Calm, offline, and always solvable', '#3049D8', '#E9EDF8'],
];
for (const f of fs.readdirSync(DIR)) {
  if (/^\d\d-.*\.png$/.test(f) && !shots.some(s => s[0] + '.png' === f)) fs.unlinkSync(`${DIR}/${f}`);
}
for (const [name, title, sub, accent, tint] of shots) {
  const html = `<!doctype html><html><head><meta charset="utf-8"><style>
html,body{margin:0;width:1080px;height:1920px;overflow:hidden;font-family:"Segoe UI",Roboto,Arial,sans-serif;background:#FFFFFF}
.bg{position:absolute;inset:0;background:linear-gradient(180deg,${tint} 0%,#FFFFFF 62%)}
.cap{position:absolute;left:0;right:0;top:96px;text-align:center;padding:0 70px}
.t{color:#1D2A4D;font-size:60px;font-weight:900;line-height:1.1;letter-spacing:.3px}
.s{color:${accent};font-size:31px;font-weight:600;margin-top:16px}
.ph{position:absolute;left:50%;top:330px;transform:translateX(-50%);width:702px;height:1560px;border-radius:52px;overflow:hidden;border:6px solid #FFFFFF;box-shadow:0 26px 70px rgba(29,42,77,.22),0 0 0 2px #DCE1EA}
.ph img{width:702px;height:1560px;display:block}
</style></head><body><div class="bg"></div>
<div class="cap"><div class="t">${title}</div><div class="s">${sub}</div></div>
<div class="ph"><img src="file:///${DIR}/src/${name}.png"></div>
</body></html>`;
  const htmlPath = `${DIR}/${name}.html`;
  fs.writeFileSync(htmlPath, html, 'utf8');
  const out = `${DIR}/${name}.png`.replace(/\//g, '\\');
  execSync(`"${CHROME}" --headless=new --disable-gpu --hide-scrollbars --no-sandbox --window-size=1080,1920 --screenshot="${out}" "file:///${DIR}/${name}.html"`, { stdio: 'ignore', timeout: 90000 });
  const b = fs.readFileSync(`${DIR}/${name}.png`);
  console.log(name, b.readUInt32BE(16) + 'x' + b.readUInt32BE(20), Math.round(b.length / 1024) + ' KB');
  fs.unlinkSync(htmlPath);
}
