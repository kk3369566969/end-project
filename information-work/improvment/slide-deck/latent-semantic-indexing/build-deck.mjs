import fs from "node:fs/promises";
import path from "node:path";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const root = "D:/work/end-project/information-work/improvment/slide-deck/latent-semantic-indexing";
const buildDir = path.join(root, ".build");
const previewDir = path.join(root, "preview");
const stagingDir = path.join(root, ".codex-finalizer");
const outputDir = root;
const draftPath = path.join(buildDir, "latent-semantic-indexing-draft.pptx");
const finalPath = path.join(outputDir, "latent-semantic-indexing.pptx");

await fs.mkdir(buildDir, { recursive: true });
await fs.mkdir(previewDir, { recursive: true });
await fs.mkdir(stagingDir, { recursive: true });

const C = {
  cream: "#F5F0E6",
  paper: "#F5F0E1",
  maroon: "#5D3A3A",
  maroon2: "#722F37",
  black: "#1A1A1A",
  teal: "#2F7373",
  brown: "#8B7355",
  charcoal: "#2D2D2D",
  tealLight: "#DCEBE8",
  maroonLight: "#F0E1E0",
  brownLight: "#E9E0D1",
  white: "#FFFCF6",
  muted: "#6A5A55",
};
const FONT = "Noto Sans SC";
const W = 1280;
const H = 720;

const presentation = Presentation.create({ slideSize: { width: W, height: H } });

function shape(slide, geometry, position, fill = "none", lineFill = "none", lineWidth = 0, name) {
  return slide.shapes.add({
    geometry,
    name,
    position,
    fill,
    line: { style: "solid", fill: lineFill, width: lineWidth },
  });
}

function text(slide, value, position, style = {}, name) {
  const s = slide.shapes.add({
    geometry: "textbox",
    name,
    position,
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  s.text = value;
  s.text.insets = 0;
  s.text.style = {
    fontFamily: FONT,
    fontSize: 18,
    color: C.black,
    ...style,
  };
  return s;
}

function line(slide, x1, y1, x2, y2, color = C.charcoal, width = 2, name) {
  const left = Math.min(x1, x2);
  const top = Math.min(y1, y2);
  return shape(slide, "line", { left, top, width: Math.abs(x2 - x1), height: Math.abs(y2 - y1) }, "none", color, width, name);
}

function addGrid(slide) {
  for (let x = 32; x < W; x += 48) line(slide, x, 0, x, H, "#EDE5D7", 0.5);
  for (let y = 24; y < H; y += 48) line(slide, 0, y, W, y, "#EDE5D7", 0.5);
}

function baseSlide(title, subtitle, source) {
  const slide = presentation.slides.add();
  slide.background.fill = C.cream;
  addGrid(slide);
  text(slide, title, { left: 64, top: 42, width: 920, height: 56 }, { fontSize: 31, bold: true, color: C.maroon }, "title");
  if (subtitle) text(slide, subtitle, { left: 66, top: 100, width: 920, height: 34 }, { fontSize: 16, color: C.muted }, "subtitle");
  text(slide, "ACADEMIC READING / 论文解读", { left: 1010, top: 48, width: 202, height: 24 }, { fontSize: 11, bold: true, color: C.teal, alignment: "right" }, "eyebrow");
  slide.speakerNotes.text = source;
  slide.speakerNotes.setVisible(true);
  return slide;
}

function panel(slide, x, y, w, h, fill = C.paper, stroke = C.brown) {
  return shape(slide, "roundRect", { left: x, top: y, width: w, height: h }, fill, stroke, 1.2);
}

function pill(slide, label, x, y, w, fill = C.tealLight, color = C.teal) {
  panel(slide, x, y, w, 28, fill, fill);
  text(slide, label, { left: x + 10, top: y + 5, width: w - 20, height: 18 }, { fontSize: 11, bold: true, color, alignment: "center" });
}

function bulletText(slide, items, x, y, w, lineHeight = 34, size = 18) {
  items.forEach((item, i) => {
    shape(slide, "ellipse", { left: x, top: y + i * lineHeight + 8, width: 7, height: 7 }, C.teal, C.teal, 0);
    text(slide, item, { left: x + 18, top: y + i * lineHeight, width: w - 18, height: lineHeight - 2 }, { fontSize: size, color: C.black });
  });
}

function addCurve(slide, x, y, w, h, color, dashed = false) {
  const cmds = [
    { moveTo: { x: 0, y: h * 0.73 } },
    { cubicBezTo: { x1: w * 0.15, y1: h * 0.62, x2: w * 0.18, y2: h * 0.34, x: w * 0.34, y: h * 0.34 } },
    { cubicBezTo: { x1: w * 0.48, y1: h * 0.34, x2: w * 0.53, y2: h * 0.43, x: w * 0.65, y: h * 0.46 } },
    { cubicBezTo: { x1: w * 0.78, y1: h * 0.5, x2: w * 0.88, y2: h * 0.63, x: w, y: h * 0.78 } },
  ];
  return slide.shapes.add({
    geometry: "custom",
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: dashed ? "dash" : "solid", fill: color, width: 4 },
    customPaths: [{ width: w, height: h, commands: cmds }],
  });
}

function matrix(slide, x, y, cols, rows, cellW, cellH, colors = [C.tealLight, C.brownLight, C.maroonLight]) {
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      const fill = colors[(r * 3 + c * 2) % colors.length];
      shape(slide, "rect", { left: x + c * cellW, top: y + r * cellH, width: cellW - 2, height: cellH - 2 }, fill, C.paper, 1);
    }
  }
}

// 1. Cover
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  addGrid(s);
  text(s, "当搜索词和文献说不同的语言", { left: 74, top: 92, width: 690, height: 105 }, { fontSize: 44, bold: true, color: C.maroon }, "cover-title");
  text(s, "Improving the retrieval of information from external sources", { left: 78, top: 210, width: 620, height: 34 }, { fontSize: 19, color: C.black });
  text(s, "Susan T. Dumais (1991)", { left: 78, top: 250, width: 320, height: 28 }, { fontSize: 16, bold: true, color: C.teal });
  pill(s, "ACADEMIC READING / 论文解读", 78, 300, 220, C.maroonLight, C.maroon2);
  text(s, "从词汇匹配到潜在语义索引（LSI）", { left: 78, top: 354, width: 560, height: 48 }, { fontSize: 25, bold: true, color: C.black });
  panel(s, 742, 94, 460, 490, C.paper, C.brown);
  text(s, "QUERY 查询", { left: 780, top: 128, width: 160, height: 24 }, { fontSize: 13, bold: true, color: C.teal });
  pill(s, "retrieval", 780, 162, 150);
  pill(s, "information", 948, 162, 160, C.brownLight, C.brown);
  text(s, "DOCUMENTS 文档", { left: 780, top: 224, width: 200, height: 24 }, { fontSize: 13, bold: true, color: C.maroon2 });
  panel(s, 780, 258, 165, 80, C.white, C.maroon2);
  text(s, "semantic search", { left: 795, top: 282, width: 135, height: 24 }, { fontSize: 15, color: C.black, alignment: "center" });
  panel(s, 978, 258, 165, 80, C.white, C.teal);
  text(s, "meaning search", { left: 993, top: 282, width: 135, height: 24 }, { fontSize: 15, color: C.black, alignment: "center" });
  line(s, 944, 298, 978, 298, C.teal, 4);
  text(s, "LATENT SPACE 潜在空间", { left: 780, top: 386, width: 250, height: 24 }, { fontSize: 13, bold: true, color: C.teal });
  line(s, 808, 520, 1128, 520, C.charcoal, 1.5); line(s, 850, 424, 850, 548, C.charcoal, 1.5);
  shape(s, "ellipse", { left: 900, top: 457, width: 18, height: 18 }, C.teal, C.teal, 1);
  shape(s, "ellipse", { left: 978, top: 474, width: 18, height: 18 }, C.teal, C.teal, 1);
  shape(s, "ellipse", { left: 1012, top: 445, width: 18, height: 18 }, C.maroon2, C.maroon2, 1);
  text(s, "terms", { left: 1040, top: 440, width: 70, height: 22 }, { fontSize: 13, color: C.maroon2 });
  line(s, 914, 466, 1019, 454, C.teal, 2);
  s.speakerNotes.text = "Source: Dumais (1991), Improving the retrieval of information from external sources.";
  s.speakerNotes.setVisible(true);
}

// 2. Problem and research question
{
  const s = baseSlide("从词汇错位到研究问题", "论文先解释为什么“找不到”，再问能否恢复潜在结构", "Source: Dumais (1991); legal retrieval example reports 20% of known relevant material found.");
  panel(s, 68, 160, 560, 450, C.paper, C.brown);
  pill(s, "THE PROBLEM / 词汇错位", 96, 184, 220, C.maroonLight, C.maroon2);
  text(s, "同一个概念，不同的词", { left: 102, top: 244, width: 300, height: 30 }, { fontSize: 22, bold: true, color: C.maroon });
  pill(s, "retrieval", 102, 292, 138);
  pill(s, "search", 268, 292, 118, C.brownLight, C.brown);
  line(s, 240, 306, 268, 306, C.maroon2, 2);
  text(s, "作者、索引者、搜索者\n使用不同词语", { left: 102, top: 350, width: 270, height: 65 }, { fontSize: 19, color: C.black });
  text(s, "SYNONYMY 同义词", { left: 102, top: 446, width: 220, height: 24 }, { fontSize: 13, bold: true, color: C.teal });
  line(s, 102, 484, 340, 484, C.teal, 3);
  text(s, "POLYSEMY 多义词", { left: 384, top: 446, width: 220, height: 24 }, { fontSize: 13, bold: true, color: C.maroon2 });
  line(s, 384, 484, 574, 484, C.maroon2, 3);
  text(s, "一个词，多个对象", { left: 384, top: 518, width: 200, height: 30 }, { fontSize: 18, color: C.black });
  panel(s, 674, 160, 528, 450, C.maroonLight, C.maroon2);
  pill(s, "PAPER QUESTION / 论文问题", 716, 184, 250, C.white, C.maroon2);
  text(s, "为什么相关材料会被漏掉？", { left: 716, top: 244, width: 430, height: 36 }, { fontSize: 26, bold: true, color: C.maroon });
  text(s, "法律检索研究", { left: 720, top: 322, width: 240, height: 28 }, { fontSize: 18, color: C.black });
  text(s, "20%", { left: 720, top: 350, width: 190, height: 86 }, { fontSize: 66, bold: true, color: C.maroon2 });
  text(s, "已知相关材料被找到\n40,000 documents / 350,000 pages", { left: 914, top: 370, width: 240, height: 68 }, { fontSize: 17, color: C.black });
  line(s, 718, 468, 1150, 468, C.brown, 1.5);
  text(s, "论文的判断：检索失败首先是表示问题，\n然后才是排序问题。", { left: 720, top: 500, width: 420, height: 54 }, { fontSize: 20, bold: true, color: C.maroon2 });
}

// 3. LSI mechanism
{
  const s = baseSlide("LSI 把“词-文档关系”压缩成共享空间", "先保留主要共现结构，再表示词、文档和查询", "Source: Dumais (1991), LSI overview and SVD formulation.");
  pill(s, "1  TERM-DOCUMENT MATRIX 词-文档矩阵", 76, 158, 280);
  matrix(s, 100, 224, 8, 6, 30, 28);
  pill(s, "2  SVD 分解", 462, 158, 160, C.brownLight, C.brown);
  text(s, "X ≈ Tₖ Sₖ Oₖᵀ", { left: 448, top: 257, width: 220, height: 42 }, { fontSize: 27, bold: true, color: C.maroon, alignment: "center" });
  matrix(s, 460, 330, 3, 6, 30, 28, [C.tealLight, C.brownLight]);
  matrix(s, 570, 330, 5, 3, 30, 28, [C.maroonLight, C.brownLight]);
  pill(s, "3  LATENT SPACE 潜在空间", 820, 158, 240, C.tealLight, C.teal);
  line(s, 860, 466, 1125, 466, C.charcoal, 1.5); line(s, 900, 232, 900, 520, C.charcoal, 1.5);
  [[960,390,C.teal],[1030,344,C.teal],[1068,418,C.maroon2],[1005,450,C.brown]].forEach(([x,y,c])=>shape(s,"ellipse",{left:x,top:y,width:18,height:18},c,c,1));
  text(s, "低维空间抑制部分噪声，让近义表达可以靠近", { left: 760, top: 558, width: 410, height: 42 }, { fontSize: 16, color: C.black, alignment: "center" });
  text(s, "保留大奇异值", { left: 470, top: 500, width: 210, height: 26 }, { fontSize: 16, color: C.teal, alignment: "center" });
  text(s, "丢弃小奇异值带来的部分噪声", { left: 430, top: 534, width: 290, height: 26 }, { fontSize: 15, color: C.muted, alignment: "center" });
}

// 4. Shared space
{
  const s = baseSlide("语义空间如何改变一次检索", "查询和文档可以比较关系，而不必共享原词", "Source: Dumais (1991), shared k-dimensional space and cosine similarity retrieval.");
  panel(s, 72, 156, 710, 460, C.paper, C.brown);
  line(s, 130, 550, 720, 550, C.charcoal, 1.5); line(s, 182, 212, 182, 570, C.charcoal, 1.5);
  line(s, 182, 550, 610, 278, C.maroon2, 4);
  text(s, "QUERY 查询", { left: 588, top: 250, width: 140, height: 26 }, { fontSize: 14, bold: true, color: C.maroon2 });
  shape(s, "ellipse", { left: 560, top: 300, width: 22, height: 22 }, C.teal, C.teal, 1);
  shape(s, "ellipse", { left: 486, top: 372, width: 22, height: 22 }, C.teal, C.teal, 1);
  shape(s, "ellipse", { left: 340, top: 430, width: 22, height: 22 }, C.maroon2, C.maroon2, 1);
  text(s, "nearby document\n无原词重合", { left: 500, top: 398, width: 180, height: 46 }, { fontSize: 15, color: C.teal });
  text(s, "shared word\n但位置更远", { left: 250, top: 470, width: 170, height: 46 }, { fontSize: 15, color: C.maroon2 });
  text(s, "cosine similarity", { left: 365, top: 318, width: 190, height: 26 }, { fontSize: 16, bold: true, color: C.teal, alignment: "center" });
  panel(s, 830, 176, 360, 400, C.maroonLight, C.maroon2);
  text(s, "排序逻辑", { left: 870, top: 214, width: 280, height: 30 }, { fontSize: 23, bold: true, color: C.maroon2 });
  bulletText(s, ["查询向量由词向量组合而成", "文档按夹角相似度排序", "词和文档共处同一 k 维空间"], 870, 278, 280, 54, 18);
  text(s, "我的读法", { left: 870, top: 470, width: 120, height: 24 }, { fontSize: 14, bold: true, color: C.teal });
  text(s, "LSI 将“找同样的词”改写为\n“找相近的关系模式”。", { left: 870, top: 500, width: 260, height: 56 }, { fontSize: 18, bold: true, color: C.black });
}

// 5. Experimental evidence
{
  const s = baseSlide("论文的核心证据：语义表示确实改善了检索", "五组集合、两个指标，以及一条需要保留的边界", "Source: Dumais (1991), Table 1, Figure 1, and reported averages.");
  const cats = ["MED", "CISI", "CRAN", "TIME", "ADI"];
  const docs = [1033, 1460, 1400, 425, 82];
  const queries = [30, 35, 225, 83, 35];
  collectionBars(s, cats, docs, queries, 70, 162, 650, 360);
  panel(s, 760, 162, 440, 132, C.maroonLight, C.maroon2);
  text(s, "约 30%", { left: 790, top: 182, width: 180, height: 58 }, { fontSize: 44, bold: true, color: C.maroon2 });
  text(s, "MED 上的改进", { left: 976, top: 200, width: 170, height: 28 }, { fontSize: 19, bold: true, color: C.black });
  panel(s, 760, 318, 440, 128, C.tealLight, C.teal);
  text(s, "约 20%", { left: 790, top: 338, width: 180, height: 58 }, { fontSize: 44, bold: true, color: C.teal });
  text(s, "跨集合平均优势", { left: 976, top: 356, width: 180, height: 28 }, { fontSize: 19, bold: true, color: C.black });
  panel(s, 760, 470, 440, 112, C.paper, C.brown);
  text(s, "精确率随召回率增加而下降，\nLSI 缓解漏检，但没有消除权衡。", { left: 790, top: 494, width: 380, height: 56 }, { fontSize: 18, color: C.black, alignment: "center" });
}

// 6. Boundaries of the paper
{
  const s = baseSlide("论文结果的三个边界", "模型收益来自参数选择、输入权重和用户反馈的共同作用", "Source: Dumais (1991), dimensionality, weighting, and relevance-feedback experiments.");
  panel(s, 70, 170, 350, 390, C.paper, C.brown);
  pill(s, "01  DIMENSION 维度", 98, 198, 190, C.maroonLight, C.maroon2);
  addCurve(s, 110, 280, 270, 150, C.teal, false);
  line(s, 110, 394, 380, 394, C.brown, 2);
  text(s, "70-100 维附近出现峰值", { left: 102, top: 452, width: 285, height: 28 }, { fontSize: 20, bold: true, color: C.maroon2, alignment: "center" });
  text(s, "过少丢失结构，过多重新吸收噪声。", { left: 102, top: 500, width: 285, height: 42 }, { fontSize: 16, color: C.muted, alignment: "center" });
  panel(s, 465, 170, 350, 390, C.tealLight, C.teal);
  pill(s, "02  WEIGHTING 加权", 493, 198, 190, C.white, C.teal);
  ["Raw TF", "Entropy", "LogEntropy"].forEach((m, i) => {
    const y = 292 + i * 50;
    text(s, m, { left: 505, top: y, width: 120, height: 24 }, { fontSize: 17, bold: i === 2, color: i === 2 ? C.teal : C.black });
    line(s, 625, y + 13, 625 + [130, 210, 270][i], y + 13, i === 2 ? C.teal : C.brown, 12);
  });
  text(s, "CRAN 中 LogEntropy 平均优势约 40%", { left: 493, top: 474, width: 285, height: 28 }, { fontSize: 18, bold: true, color: C.teal, alignment: "center" });
  text(s, "输入矩阵的质量，先约束了潜在空间。", { left: 493, top: 516, width: 285, height: 34 }, { fontSize: 15, color: C.muted, alignment: "center" });
  panel(s, 860, 170, 340, 390, C.maroonLight, C.maroon2);
  pill(s, "03  FEEDBACK 反馈", 888, 198, 190, C.white, C.maroon2);
  const fb = [["查询", 904, 292], ["结果", 1052, 292], ["判断", 1052, 410], ["更新", 904, 410]];
  fb.forEach(([label, x, y]) => { shape(s, "ellipse", { left: x, top: y, width: 62, height: 62 }, C.white, C.maroon2, 1.5); text(s, label, { left: x, top: y + 20, width: 62, height: 22 }, { fontSize: 16, bold: true, color: C.maroon2, alignment: "center" }); });
  line(s, 966, 323, 1052, 323, C.maroon2, 2); line(s, 1083, 354, 1083, 410, C.maroon2, 2); line(s, 1052, 441, 966, 441, C.maroon2, 2); line(s, 935, 410, 935, 354, C.maroon2, 2);
  text(s, "第一个相关文档约 +33%\n前三个相关文档约 +67%", { left: 890, top: 490, width: 280, height: 52 }, { fontSize: 17, bold: true, color: C.maroon2, alignment: "center" });
}

function slideChart(slide, categories, series, left, top, width, height, title) {
  return slide.charts.add("bar", {
    position: { left, top, width, height },
    title,
    titleTextStyle: { fontSize: 17, fill: C.maroon, bold: true },
    categories,
    series,
    hasLegend: true,
    legend: { position: "bottom", textStyle: { fontSize: 12, fill: C.muted } },
    barOptions: { direction: "column", grouping: "clustered", gapWidth: 55 },
    xAxis: { textStyle: { fontSize: 13, fill: C.black }, line: { style: "solid", fill: C.charcoal, width: 1 } },
    yAxis: { textStyle: { fontSize: 12, fill: C.muted }, majorGridlines: { style: "solid", fill: "#E6DED0", width: 1 }, line: { style: "solid", fill: C.charcoal, width: 1 } },
    chartFill: C.paper,
    plotAreaFill: C.paper,
  });
}

function collectionBars(slide, cats, docs, queries, left, top, width, height) {
  panel(slide, left, top, width, height, C.paper, C.brown);
  text(slide, "Documents / Queries", { left: left + 26, top: top + 22, width: 250, height: 24 }, { fontSize: 17, bold: true, color: C.maroon });
  const baseY = top + height - 62;
  const scale = 0.00019;
  const groupW = 104;
  cats.forEach((cat, i) => {
    const x = left + 44 + i * groupW;
    const docH = Math.max(8, docs[i] * scale * 200);
    const qH = Math.max(8, queries[i] * scale * 200);
    shape(slide, "rect", { left: x, top: baseY - docH, width: 28, height: docH }, C.teal, C.teal, 1);
    shape(slide, "rect", { left: x + 34, top: baseY - qH, width: 28, height: qH }, C.maroon2, C.maroon2, 1);
    text(slide, cat, { left: x - 8, top: baseY + 12, width: 74, height: 20 }, { fontSize: 14, bold: true, color: C.black, alignment: "center" });
  });
  line(slide, left + 36, baseY, left + width - 30, baseY, C.charcoal, 1.5);
  pill(slide, "documents", left + width - 190, top + 22, 90, C.tealLight, C.teal);
  pill(slide, "queries", left + width - 92, top + 22, 70, C.maroonLight, C.maroon2);
  text(slide, "柱高按论文 Table 1 的规模比例绘制", { left: left + 26, top: top + height - 30, width: 300, height: 18 }, { fontSize: 12, color: C.muted });
}

// 7. Independent thought: representation gap
{
  const s = baseSlide("我的思考 1：检索的第一道误差发生在表示层", "排序再聪明，也无法找回从入口处被漏掉的材料", "Independent reading based on Dumais (1991). This slide separates the representation gap from the ranking gap.");
  panel(s, 72, 178, 750, 360, C.paper, C.brown);
  const stages = [
    ["用户表达", "search / retrieval", 112, C.maroonLight, C.maroon2],
    ["集合表达", "document vocabulary", 302, C.brownLight, C.brown],
    ["模型表示", "latent space", 492, C.tealLight, C.teal],
    ["结果排序", "ranked evidence", 682, C.maroonLight, C.maroon2],
  ];
  stages.forEach(([zh, en, x, fill, stroke], i) => {
    panel(s, x, 300, 150, 96, fill, stroke);
    text(s, zh, { left: x + 12, top: 320, width: 126, height: 24 }, { fontSize: 19, bold: true, color: stroke, alignment: "center" });
    text(s, en, { left: x + 10, top: 352, width: 130, height: 20 }, { fontSize: 13, color: C.muted, alignment: "center" });
    if (i < stages.length - 1) line(s, x + 150, 348, x + 190, 348, C.teal, 3);
  });
  text(s, "表示错位", { left: 230, top: 238, width: 180, height: 24 }, { fontSize: 15, bold: true, color: C.maroon2, alignment: "center" });
  text(s, "LSI 的价值在这里：\n先让不同词语进入可比较的空间。", { left: 420, top: 432, width: 360, height: 54 }, { fontSize: 19, bold: true, color: C.black, alignment: "center" });
  panel(s, 872, 178, 320, 360, C.maroonLight, C.maroon2);
  pill(s, "我的判断", 906, 208, 150, C.white, C.maroon2);
  text(s, "01", { left: 906, top: 270, width: 44, height: 32 }, { fontSize: 26, bold: true, color: C.maroon2 });
  text(s, "很多“排序问题”，其实是表示问题。", { left: 960, top: 270, width: 190, height: 50 }, { fontSize: 17, color: C.black });
  text(s, "02", { left: 906, top: 356, width: 44, height: 32 }, { fontSize: 26, bold: true, color: C.maroon2 });
  text(s, "提升召回率，不能自动保证相关性。", { left: 960, top: 356, width: 190, height: 50 }, { fontSize: 17, color: C.black });
  text(s, "03", { left: 906, top: 442, width: 44, height: 32 }, { fontSize: 26, bold: true, color: C.maroon2 });
  text(s, "评价系统要先问：它看见了什么？", { left: 960, top: 442, width: 190, height: 50 }, { fontSize: 17, color: C.black });
}

// 8. Independent thought: LSI and embeddings
{
  const s = baseSlide("我的思考 2：LSI 与今天的 embedding，问题相同但能力不同", "两者都在学习相似关系，但学习来源、表达能力和风险不同", "Independent comparison based on Dumais (1991) and current retrieval concepts; this is a conceptual comparison, not a historical equivalence claim.");
  panel(s, 72, 170, 520, 410, C.paper, C.brown);
  pill(s, "LSI / 论文里的答案", 106, 198, 220, C.maroonLight, C.maroon2);
  const lsi = [
    ["学习来源", "当前文档集合的共现矩阵"],
    ["表示方式", "线性降维，维度 k 可调"],
    ["优点", "成本低，结构相对可追溯"],
    ["风险", "换集合后潜在空间会变化"],
  ];
  lsi.forEach(([label, body], i) => {
    const y = 260 + i * 70;
    text(s, label, { left: 110, top: y, width: 90, height: 24 }, { fontSize: 15, bold: true, color: C.teal });
    text(s, body, { left: 210, top: y, width: 330, height: 34 }, { fontSize: 16, color: C.black });
    if (i < lsi.length - 1) line(s, 110, y + 48, 540, y + 48, "#E6DED0", 1);
  });
  panel(s, 648, 170, 544, 410, C.tealLight, C.teal);
  pill(s, "EMBEDDING / 今天常见的答案", 684, 198, 250, C.white, C.teal);
  const emb = [
    ["学习来源", "更大规模语料与预训练任务"],
    ["表示方式", "非线性、上下文相关的向量"],
    ["优点", "跨领域迁移，表达能力更强"],
    ["风险", "训练偏差和相似度更难解释"],
  ];
  emb.forEach(([label, body], i) => {
    const y = 260 + i * 70;
    text(s, label, { left: 692, top: y, width: 90, height: 24 }, { fontSize: 15, bold: true, color: C.teal });
    text(s, body, { left: 792, top: y, width: 350, height: 34 }, { fontSize: 16, color: C.black });
    if (i < emb.length - 1) line(s, 692, y + 48, 1142, y + 48, "#C9DFDA", 1);
  });
  text(s, "我的结论：技术换代了，但“表示什么、丢掉什么、如何验证”仍然是同一组问题。", { left: 102, top: 612, width: 1060, height: 30 }, { fontSize: 18, bold: true, color: C.maroon2, alignment: "center" });
}

// 9. Independent thought: feedback and user cost
{
  const s = baseSlide("我的思考 3：反馈的价值，不只在提升指标", "用户标记一个相关文档，也是在帮助系统理解任务边界", "Independent reading based on Dumais (1991) relevance feedback results and the user's interaction cost.");
  panel(s, 72, 172, 760, 250, C.paper, C.brown);
  const steps = [["提交查询", 110], ["浏览结果", 300], ["标记相关", 490], ["修正表达", 680]];
  steps.forEach(([label, x], i) => {
    shape(s, "ellipse", { left: x, top: 252, width: 70, height: 70 }, i === 2 ? C.maroonLight : C.tealLight, i === 2 ? C.maroon2 : C.teal, 2);
    text(s, String(i + 1), { left: x, top: 271, width: 70, height: 28 }, { fontSize: 24, bold: true, color: i === 2 ? C.maroon2 : C.teal, alignment: "center" });
    text(s, label, { left: x - 20, top: 340, width: 110, height: 24 }, { fontSize: 16, bold: true, color: C.black, alignment: "center" });
    if (i < steps.length - 1) line(s, x + 70, 287, x + 190, 287, C.teal, 3);
  });
  panel(s, 72, 458, 360, 128, C.maroonLight, C.maroon2);
  text(s, "+33% / +67%", { left: 102, top: 480, width: 300, height: 46 }, { fontSize: 34, bold: true, color: C.maroon2, alignment: "center" });
  text(s, "论文测到的是反馈后的平均收益", { left: 102, top: 536, width: 300, height: 22 }, { fontSize: 15, color: C.black, alignment: "center" });
  panel(s, 462, 458, 370, 128, C.tealLight, C.teal);
  text(s, "中位数浏览量：7 个文档", { left: 492, top: 482, width: 310, height: 28 }, { fontSize: 22, bold: true, color: C.teal, alignment: "center" });
  text(s, "收益来自用户愿意继续判断", { left: 492, top: 536, width: 310, height: 22 }, { fontSize: 15, color: C.black, alignment: "center" });
  panel(s, 872, 172, 320, 414, C.maroonLight, C.maroon2);
  pill(s, "我的判断", 906, 202, 150, C.white, C.maroon2);
  text(s, "反馈是一种\n“共同建模”", { left: 906, top: 260, width: 250, height: 70 }, { fontSize: 28, bold: true, color: C.maroon2, alignment: "center" });
  line(s, 906, 356, 1158, 356, C.brown, 1.5);
  text(s, "系统获得更多证据，\n用户也承担更多认知成本。", { left: 906, top: 390, width: 250, height: 60 }, { fontSize: 19, bold: true, color: C.black, alignment: "center" });
  text(s, "所以反馈入口要短、可撤回、能解释下一步变化。", { left: 906, top: 500, width: 250, height: 48 }, { fontSize: 15, color: C.muted, alignment: "center" });
}

// 10. Independent thought: external validity
{
  const s = baseSlide("我的反思：论文的提升不能直接等同于真实世界效果", "实验结果很有价值，但它回答的是“在这些条件下是否改善”", "Independent critique of the paper's evidence scope, based on its collections, relevance judgments, and parameter sensitivity.");
  panel(s, 72, 172, 720, 408, C.paper, C.brown);
  const caveats = [
    ["集合", "MED、CISI 等语料有明确领域与规模", C.tealLight, C.teal],
    ["判断", "相关性标签把复杂任务压成对错", C.maroonLight, C.maroon2],
    ["参数", "维度与权重改变结果，最佳值不固定", C.brownLight, C.brown],
    ["解释", "相似度提升不等于用户理解提升", C.tealLight, C.teal],
  ];
  caveats.forEach(([label, body, fill, stroke], i) => {
    const x = 104 + (i % 2) * 330;
    const y = 220 + Math.floor(i / 2) * 152;
    panel(s, x, y, 290, 112, fill, stroke);
    text(s, label, { left: x + 18, top: y + 18, width: 70, height: 24 }, { fontSize: 18, bold: true, color: stroke });
    text(s, body, { left: x + 18, top: y + 52, width: 250, height: 44 }, { fontSize: 16, color: C.black });
  });
  panel(s, 842, 172, 350, 408, C.maroonLight, C.maroon2);
  pill(s, "我会怎么读结果", 878, 202, 200, C.white, C.maroon2);
  text(s, "先看平均值", { left: 878, top: 270, width: 250, height: 26 }, { fontSize: 20, bold: true, color: C.maroon2 });
  text(s, "确认总体方向是否改善。", { left: 878, top: 304, width: 250, height: 24 }, { fontSize: 16, color: C.black });
  text(s, "再看边界", { left: 878, top: 360, width: 250, height: 26 }, { fontSize: 20, bold: true, color: C.maroon2 });
  text(s, "哪些集合、参数和用户任务没有受益？", { left: 878, top: 394, width: 250, height: 44 }, { fontSize: 16, color: C.black });
  text(s, "最后问成本", { left: 878, top: 468, width: 250, height: 26 }, { fontSize: 20, bold: true, color: C.maroon2 });
  text(s, "用户是否更快找到答案，而不只是得到更多结果？", { left: 878, top: 502, width: 250, height: 48 }, { fontSize: 16, color: C.black });
}

// 11. Independent thought: proposed retrieval loop
{
  const s = baseSlide("我的主张：把检索设计成“共同建模”", "系统给出候选，用户提供判断，双方一起修正问题", "Independent design proposal based on the paper's representation, weighting, and relevance-feedback ideas.");
  const stages = [
    ["表示", "把不同说法放进同一空间", C.tealLight, C.teal],
    ["证据", "让结果展示为什么被召回", C.brownLight, C.brown],
    ["判断", "让用户低成本表达相关性", C.maroonLight, C.maroon2],
  ];
  stages.forEach(([label, body, fill, stroke], i) => {
    const x = 92 + i * 370;
    panel(s, x, 206, 300, 180, fill, stroke);
    text(s, String(i + 1).padStart(2, "0"), { left: x + 24, top: 230, width: 50, height: 36 }, { fontSize: 30, bold: true, color: stroke });
    text(s, label, { left: x + 92, top: 236, width: 150, height: 30 }, { fontSize: 24, bold: true, color: stroke });
    text(s, body, { left: x + 24, top: 304, width: 250, height: 44 }, { fontSize: 18, color: C.black, alignment: "center" });
    if (i < 2) line(s, x + 300, 296, x + 370, 296, C.teal, 3);
  });
  panel(s, 92, 458, 1040, 116, C.paper, C.brown);
  text(s, "我认为一个更好的检索系统，至少应该满足三点：", { left: 122, top: 480, width: 410, height: 26 }, { fontSize: 18, bold: true, color: C.maroon });
  text(s, "相似度可解释", { left: 560, top: 480, width: 150, height: 24 }, { fontSize: 17, bold: true, color: C.teal, alignment: "center" });
  text(s, "反馈可撤回", { left: 750, top: 480, width: 150, height: 24 }, { fontSize: 17, bold: true, color: C.maroon2, alignment: "center" });
  text(s, "失败可复盘", { left: 940, top: 480, width: 150, height: 24 }, { fontSize: 17, bold: true, color: C.brown, alignment: "center" });
  line(s, 548, 518, 720, 518, C.teal, 2); line(s, 738, 518, 910, 518, C.maroon2, 2); line(s, 928, 518, 1100, 518, C.brown, 2);
  text(s, "这不是把用户变成标注员，而是让系统把不确定性展示出来。", { left: 122, top: 546, width: 410, height: 24 }, { fontSize: 15, color: C.muted });
}

// 12. Back cover
{
  const s = presentation.slides.add();
  s.background.fill = C.cream;
  addGrid(s);
  text(s, "我的个人总结：好的检索，\n是逐步逼近正确的问题", { left: 74, top: 112, width: 680, height: 132 }, { fontSize: 40, bold: true, color: C.maroon }, "closing-title");
  text(s, "论文结论", { left: 82, top: 316, width: 130, height: 24 }, { fontSize: 15, bold: true, color: C.teal });
  text(s, "潜在结构能提升检索，但参数和反馈决定实际收益。", { left: 82, top: 346, width: 590, height: 30 }, { fontSize: 20, color: C.black });
  text(s, "我的反思", { left: 82, top: 406, width: 130, height: 24 }, { fontSize: 15, bold: true, color: C.maroon2 });
  text(s, "评价检索系统时，要同时问“找到了什么”和“用户付出了什么”。", { left: 82, top: 436, width: 610, height: 58 }, { fontSize: 20, color: C.black });
  text(s, "下一步", { left: 82, top: 520, width: 130, height: 24 }, { fontSize: 15, bold: true, color: C.brown });
  text(s, "把语义表示、透明反馈和失败案例分析放在同一个闭环里。", { left: 82, top: 550, width: 610, height: 30 }, { fontSize: 20, color: C.black });
  text(s, "个人总结：好的检索系统应当帮助用户形成更好的问题，而不只是返回更多结果。", { left: 82, top: 596, width: 620, height: 28 }, { fontSize: 15, color: C.muted });
  panel(s, 790, 150, 380, 400, C.paper, C.brown);
  text(s, "READ THE RELATIONSHIPS,\nTHEN CHECK THE EVIDENCE", { left: 824, top: 180, width: 312, height: 58 }, { fontSize: 19, bold: true, color: C.maroon2, alignment: "center" });
  const checkpoints = [["潜在结构", 840, 300, C.teal], ["证据", 1005, 372, C.brown], ["用户判断", 865, 472, C.maroon2]];
  checkpoints.forEach(([label, x, y, c]) => { shape(s, "ellipse", { left: x, top: y, width: 28, height: 28 }, c, c, 1); text(s, label, { left: x - 30, top: y + 36, width: 100, height: 22 }, { fontSize: 15, color: C.black, alignment: "center" }); });
  line(s, 868, 314, 1018, 385, C.teal, 3); line(s, 1018, 400, 878, 485, C.brown, 3);
  shape(s, "ellipse", { left: 1075, top: 475, width: 55, height: 55 }, C.tealLight, C.teal, 2);
  text(s, "相关\n文档", { left: 1082, top: 486, width: 40, height: 34 }, { fontSize: 13, bold: true, color: C.teal, alignment: "center" });
  s.speakerNotes.text = "Closing synthesis based on Dumais (1991) and independent reflection.";
  s.speakerNotes.setVisible(true);
}

const speakerNotes = [
  `各位好，今天我分享的是 Susan Dumais 1991 年关于 Latent Semantic Indexing，也就是 LSI 的论文。我的分享先讲论文解决了什么问题，再讲我的理解和启发。`,
  `这篇论文从一个很实际的困难出发：用户、作者和索引者往往不会使用同一套词。同义词造成漏检，多义词造成误检。法律检索研究中，用户只找到约百分之二十的已知相关内容。论文因此追问：能不能从词语共同出现的模式中恢复潜在结构？`,
  `LSI 可以理解成三步：建立词与文档的关联矩阵，用 SVD 保留主要结构，最后把词、文档和查询放进同一个低维空间。它不要求每个维度都能被命名，更关心这个压缩后的空间能不能改善检索。`,
  `查询和文档比较的是向量关系，而不只是有没有相同的字面词。一个没有共享原词的文档，只要整体使用模式相近，也可能被召回。LSI 把“找相同的词”改写成了“找相近的关系模式”。`,
  `论文用了五组集合，同时观察精确率和召回率。MED 上约百分之三十的改进、跨集合约百分之二十的平均优势，说明潜在表示确实有效。但精确率和召回率的权衡仍然存在。`,
  `论文结果还有三个边界：维度影响结构保留，词项加权影响潜在空间，相关性反馈则依赖用户继续判断。也就是说，表示、参数和交互共同决定实际收益。`,
  `这里进入我的第一个思考。很多看似是排序问题的失败，其实在排序之前就因为表示错位而被漏掉了。评价系统时，第一问应该是：它到底看见了哪些候选？`,
  `我的第二个思考，是把 LSI 和今天常见的 embedding 放在一起看。技术实现不同，但两者都要面对同一组问题：表示保留了什么、丢掉了什么，相似度是否真的对应任务需要。`,
  `我的第三个思考与反馈有关。用户标记相关文档是在告诉系统任务边界，也在承担认知成本。因此反馈入口要短、可撤回，还要解释下一轮结果为什么变化。`,
  `我对论文最大的反思是，实验中的平均提升不能直接等同于真实世界效果。集合、相关性判断、参数设置和用户任务，都可能改变结论。`,
  `基于这个判断，我更愿意把检索理解成共同建模：系统负责表示和提供证据，用户用低成本判断相关性，双方一起修正问题。`,
  `最后总结我的观点。好的检索不是返回更多相同的词，而是帮助用户逐步逼近真正的问题。谢谢大家。`,
];

for (const [index, slide] of presentation.slides.items.entries()) {
  slide.speakerNotes.text = speakerNotes[index];
  slide.speakerNotes.setVisible(true);
  const stem = `slide-${String(index + 1).padStart(2, "0")}`;
  const png = await presentation.export({ slide, format: "png", scale: 1 });
  await fs.writeFile(path.join(previewDir, `${stem}.png`), new Uint8Array(await png.arrayBuffer()));
}

const pptx = await PresentationFile.exportPptx(presentation);
await pptx.save(draftPath);
console.log(JSON.stringify({ draftPath, slides: presentation.slides.items.length, previewDir }));
