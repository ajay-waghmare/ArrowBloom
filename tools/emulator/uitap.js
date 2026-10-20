// Parse a uiautomator dump and print "label @x,y" for every node with text or content-desc.
// With a second argument, print only the centre of the first node whose label contains it (for input tap).
//   node uitap.js ui.xml            -> every label with its centre
//   node uitap.js ui.xml "Play"     -> "x y" of the first label containing "Play" (exit 1 if none)
const fs = require('fs');
const s = fs.readFileSync(process.argv[2], 'utf8');
const want = process.argv[3];
const re = /<node[^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g;
let m; const out = [];
while ((m = re.exec(s))) {
  const label = (m[1] || m[2]).replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#10;/g, ' ');
  if (!label) continue;
  const cx = Math.round((+m[3] + +m[5]) / 2), cy = Math.round((+m[4] + +m[6]) / 2);
  out.push({ label, cx, cy });
}
if (want) {
  const hit = out.find(o => o.label.toLowerCase().includes(want.toLowerCase()));
  if (!hit) { console.error('not found: ' + want); process.exit(1); }
  console.log(hit.cx + ' ' + hit.cy);
} else {
  console.log(out.map(o => `${o.label.slice(0, 40)} @${o.cx},${o.cy}`).join(' | '));
}
