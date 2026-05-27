const pptxgen = require("pptxgenjs");
const pptx = new pptxgen();
pptx.defineLayout({ name: "WIDE", width: 13.33, height: 7.5 });
pptx.layout = "WIDE";

const NAVY = "1B2A4A";
const TEAL = "2EC4B6";
const WHITE = "FFFFFF";
const LIGHT_BG = "F0F4F8";
const DARK_TEXT = "1A1A2E";
const ACCENT = "E07A5F";
const MUTED = "6B7280";

// ===== SLIDE 1: Title =====
const slide1 = pptx.addSlide();
slide1.background = { color: NAVY };
slide1.addShape(pptx.ShapeType.ellipse, { x: 9.5, y: -1.5, w: 5, h: 5, fill: { color: TEAL, transparency: 85 } });
slide1.addShape(pptx.ShapeType.ellipse, { x: -1, y: 4, w: 4, h: 4, fill: { color: ACCENT, transparency: 85 } });
slide1.addText("The Future of", { x: 0.8, y: 1.8, w: 10, h: 1.0, fontSize: 20, color: TEAL, fontFace: "Calibri", bold: true });
slide1.addText("Remote Work", { x: 0.8, y: 2.5, w: 11, h: 1.6, fontSize: 48, color: WHITE, fontFace: "Georgia", bold: true });
slide1.addText("Trends, Challenges & Opportunities in 2025 & Beyond", { x: 0.8, y: 4.2, w: 10, h: 0.7, fontSize: 16, color: "CADCFC", fontFace: "Calibri" });
slide1.addShape(pptx.ShapeType.rect, { x: 0.8, y: 5.2, w: 2.5, h: 0.06, fill: { color: TEAL } });
slide1.addText("Presented by FutureWorks Research", { x: 0.8, y: 5.6, w: 6, h: 0.5, fontSize: 12, color: MUTED, fontFace: "Calibri" });

// ===== SLIDE 2: The Big Picture =====
const slide2 = pptx.addSlide();
slide2.background = { color: LIGHT_BG };
slide2.addText("The Big Picture", { x: 0.8, y: 0.5, w: 8, h: 0.9, fontSize: 36, color: NAVY, fontFace: "Georgia", bold: true });
slide2.addText("Remote work is no longer a temporary shift — it's a permanent transformation.", { x: 0.8, y: 1.3, w: 11, h: 0.6, fontSize: 14, color: MUTED, fontFace: "Calibri" });

const stats = [
  { num: "35%", label: "of workforce fully\nremote by 2025" },
  { num: "87%", label: "of remote workers\nreport higher productivity" },
  { num: "4.7x", label: "more likely to\nretain remote talent" },
  { num: "$11K", label: "annual savings per\nremote employee" }
];

stats.forEach((s, i) => {
  const x = 0.8 + i * 3.1;
  slide2.addShape(pptx.ShapeType.roundRect, { x, y: 2.3, w: 2.8, h: 3.2, fill: { color: WHITE }, shadow: { type: "outer", blur: 6, offset: 2, color: "000000", opacity: 0.1 }, rectRadius: 0.15 });
  slide2.addText(s.num, { x, y: 2.6, w: 2.8, h: 1.2, fontSize: 40, color: TEAL, fontFace: "Georgia", bold: true, align: "center" });
  slide2.addText(s.label, { x, y: 3.8, w: 2.8, h: 1.2, fontSize: 13, color: DARK_TEXT, fontFace: "Calibri", align: "center" });
});

slide2.addText("Source: Global Workplace Analytics, 2024", { x: 0.8, y: 6.6, w: 6, h: 0.4, fontSize: 10, color: MUTED, fontFace: "Calibri", italic: true });

// ===== SLIDE 3: Key Drivers =====
const slide3 = pptx.addSlide();
slide3.background = { color: NAVY };
slide3.addText("What's Driving the Shift?", { x: 0.8, y: 0.5, w: 10, h: 0.9, fontSize: 36, color: WHITE, fontFace: "Georgia", bold: true });
slide3.addShape(pptx.ShapeType.rect, { x: 0.8, y: 1.3, w: 2, h: 0.05, fill: { color: TEAL } });

const drivers = [
  { title: "Technology", desc: "Cloud collaboration, AI assistants, and VR meeting spaces make remote work seamless and immersive." },
  { title: "Talent Competition", desc: "Companies now hire globally. Location is no longer a barrier to accessing top talent." },
  { title: "Employee Expectations", desc: "Work-life balance, flexibility, and autonomy are now non-negotiable for top performers." },
  { title: "Cost Optimization", desc: "Reduced real estate footprint and operational overhead drive significant business savings." }
];

drivers.forEach((d, i) => {
  const y = 1.8 + i * 1.35;
  slide3.addShape(pptx.ShapeType.roundRect, { x: 0.8, y, w: 11.5, h: 1.15, fill: { color: "233554" }, rectRadius: 0.08 });
  slide3.addShape(pptx.ShapeType.ellipse, { x: 1.1, y: y + 0.2, w: 0.7, h: 0.7, fill: { color: TEAL } });
  slide3.addText(String(i + 1), { x: 1.1, y: y + 0.2, w: 0.7, h: 0.7, fontSize: 18, color: NAVY, fontFace: "Georgia", bold: true, align: "center", valign: "middle" });
  slide3.addText(d.title, { x: 2.1, y: y + 0.1, w: 9.5, h: 0.45, fontSize: 18, color: WHITE, fontFace: "Calibri", bold: true });
  slide3.addText(d.desc, { x: 2.1, y: y + 0.5, w: 9.5, h: 0.5, fontSize: 13, color: "CADCFC", fontFace: "Calibri" });
});

// ===== SLIDE 4: Challenges & Solutions =====
const slide4 = pptx.addSlide();
slide4.background = { color: LIGHT_BG };
slide4.addText("Challenges & Solutions", { x: 0.8, y: 0.5, w: 10, h: 0.9, fontSize: 36, color: NAVY, fontFace: "Georgia", bold: true });

const challenges = [
  { challenge: "Team Collaboration", solution: "Async-first culture + scheduled sync touchpoints", iconColor: ACCENT },
  { challenge: "Mental Health", solution: "Wellness stipends, boundaries, and flexible hours", iconColor: TEAL },
  { challenge: "Security Risks", solution: "Zero-trust architecture and VPN infrastructure", iconColor: ACCENT },
  { challenge: "Career Growth", solution: "Structured mentorship and transparent promotion paths", iconColor: TEAL }
];

challenges.forEach((c, i) => {
  const y = 1.6 + i * 1.35;
  slide4.addShape(pptx.ShapeType.roundRect, { x: 0.8, y, w: 5.5, h: 1.15, fill: { color: WHITE }, shadow: { type: "outer", blur: 4, offset: 1, color: "000000", opacity: 0.08 }, rectRadius: 0.1 });
  slide4.addShape(pptx.ShapeType.ellipse, { x: 1.1, y: y + 0.2, w: 0.7, h: 0.7, fill: { color: c.iconColor } });
  slide4.addText("!", { x: 1.1, y: y + 0.2, w: 0.7, h: 0.7, fontSize: 20, color: WHITE, fontFace: "Georgia", bold: true, align: "center", valign: "middle" });
  slide4.addText(c.challenge, { x: 2.1, y: y + 0.15, w: 4, h: 0.45, fontSize: 17, color: DARK_TEXT, fontFace: "Calibri", bold: true });
  slide4.addText("Challenge", { x: 2.1, y: y + 0.55, w: 4, h: 0.4, fontSize: 11, color: MUTED, fontFace: "Calibri", italic: true });
  slide4.addText("→", { x: 6.5, y, w: 0.6, h: 1.15, fontSize: 28, color: TEAL, fontFace: "Calibri", align: "center", valign: "middle" });
  slide4.addShape(pptx.ShapeType.roundRect, { x: 7.2, y, w: 5.3, h: 1.15, fill: { color: "E8F8F5" }, rectRadius: 0.1 });
  slide4.addText("✓", { x: 7.4, y: y + 0.2, w: 0.5, h: 0.7, fontSize: 22, color: TEAL, fontFace: "Calibri", align: "center", valign: "middle" });
  slide4.addText("Solution", { x: 8.1, y: y + 0.1, w: 4, h: 0.35, fontSize: 11, color: TEAL, fontFace: "Calibri", italic: true, bold: true });
  slide4.addText(c.solution, { x: 8.1, y: y + 0.4, w: 4.1, h: 0.6, fontSize: 15, color: DARK_TEXT, fontFace: "Calibri" });
});

// ===== SLIDE 5: Conclusion =====
const slide5 = pptx.addSlide();
slide5.background = { color: NAVY };
slide5.addShape(pptx.ShapeType.ellipse, { x: 10, y: -2, w: 5, h: 5, fill: { color: TEAL, transparency: 85 } });
slide5.addShape(pptx.ShapeType.ellipse, { x: -1.5, y: 5, w: 4, h: 4, fill: { color: ACCENT, transparency: 85 } });
slide5.addText("The Future is Flexible", { x: 0.8, y: 1.2, w: 11, h: 1.2, fontSize: 42, color: WHITE, fontFace: "Georgia", bold: true });
slide5.addShape(pptx.ShapeType.rect, { x: 0.8, y: 2.5, w: 2.5, h: 0.06, fill: { color: TEAL } });
slide5.addText("Organizations that embrace remote work will:", { x: 0.8, y: 3.0, w: 10, h: 0.6, fontSize: 16, color: "CADCFC", fontFace: "Calibri" });

const points = [
  "Access a global talent pool without geographic limits",
  "Build more diverse, inclusive, and resilient teams",
  "Reduce operational costs while boosting productivity",
  "Attract and retain top talent through flexibility"
];

points.forEach((p, i) => {
  const y = 3.8 + i * 0.6;
  slide5.addText("✦", { x: 0.8, y, w: 0.4, h: 0.5, fontSize: 14, color: TEAL, fontFace: "Calibri" });
  slide5.addText(p, { x: 1.4, y, w: 10, h: 0.5, fontSize: 14, color: WHITE, fontFace: "Calibri" });
});

slide5.addText('"The best way to predict the future is to create it."', { x: 0.8, y: 6.2, w: 10, h: 0.6, fontSize: 14, color: TEAL, fontFace: "Georgia", italic: true });

pptx.writeFile({ fileName: "remote_work.pptx" })
  .then(() => console.log("OK: remote_work.pptx created"))
  .catch(err => console.error("ERROR:", err));
