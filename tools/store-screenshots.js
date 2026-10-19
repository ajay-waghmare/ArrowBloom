// Frames the raw emulator captures in store/screenshots/src/*.png (1080x2400) as 1080x1920 (9:16) Play
// phone screenshots with a caption, rendered by headless Chrome. Play rejects images taller than 2:1,
// so raw 1080x2400 captures cannot be uploaded as they are.
//   node tools/store-screenshots.js            (needs Chrome at the path below, or set CHROME=...)
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const DIR = path.resolve(__dirname, '../store/screenshots').replace(/\/g, '/');
const CHROME = process.env.CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const shots = [
  ['01-home', 'Tap. Clear. Bloom.', 'A calm logic puzzle with no timer'],
  ['02-level1', 'Tap an arrow with a clear path', 'It flies off the board the way it points'],
  ['03-orchard', 'Long arrows need the whole body free', 'Order matters: clear what is in front first'],
  ['04-daily', 'One Daily Bloom, the same for everyone', 'Keep a streak, share a spoiler-free result'],
  ['05-daily-outcome', 'Stars for a clean solve, never for speed', 'Every escape blooms and plays a note'],
  ['06-infinite', 'Five gardens, then Infinite Bloom', 'Every level generated and proven solvable'],
];
const accents = ['#5CF2C5', '#5CF2C5', '#FFB800', '#FF8FB1', '#FF8FB1', '#F8F4FF'];
for (const [i, [name, title, sub]] of shots.entries()) {
  const html = `<!doctype html><html><head><meta charset="utf-8"><style>
html,body{margin:0;width:1080px;height:1920px;overflow:hidden;font-family:"Segoe UI",Roboto,Arial,sans-serif;background:#0C101D}
.bg{position:absolute;inset:0;background:radial-gradient(900px 700px at 50% 20%,#1C2744 0%,#0C101D 55%,#070A14 100%)}
.cap{position:absolute;left:0;right:0;top:92px;text-align:center;padding:0 70px}
.t{color:#fff;font-size:58px;font-weight:900;line-height:1.1;letter-spacing:.5px;text-shadow:0 6px 18px rgba(0,0,0,.6)}
.s{color:${accents[i]};font-size:30px;font-weight:600;margin-top:16px}
.ph{position:absolute;left:50%;top:330px;transform:translateX(-50%);width:702px;height:1560px;border-radius:52px;overflow:hidden;border:6px solid #2E3D66;box-shadow:0 30px 80px rgba(0,0,0,.65)}
.ph img{width:702px;height:1560px;display:block}
</style></head><body><div class="bg"></div>
<div class="cap"><div class="t">${title}</div><div class="s">${sub}</div></div>
<div class="ph"><img src="file:///${DIR}/src/${name}.png"></div>
</body></html>`;
  const htmlPath = `${DIR}/${name}.html`;
  fs.writeFileSync(htmlPath, html, 'utf8');
  const out = `${DIR}/${name}.png`.replace(/\//g, '\');
  execSync(`"${CHROME}" --headless=new --disable-gpu --hide-scrollbars --no-sandbox --window-size=1080,1920 --screenshot="${out}" "file:///${DIR}/${name}.html"`, { stdio: 'ignore', timeout: 90000 });
  const b = fs.readFileSync(`${DIR}/${name}.png`);
  console.log(name, b.readUInt32BE(16) + 'x' + b.readUInt32BE(20), Math.round(b.length / 1024) + ' KB');
  fs.unlinkSync(htmlPath);
}
