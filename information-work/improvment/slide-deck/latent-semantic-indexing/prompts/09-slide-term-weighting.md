---
slide: 9
filename: 09-slide-term-weighting.png
references: []
---

Create a 16:9 Chinese slide in an academic technical briefing style. Use aged cream #F5F0E6 with subtle paper grain and faint grid, dark maroon #5D3A3A titles, near-black #1A1A1A body, teal #2F7373 best-method highlight, warm brown #8B7355 secondary annotations, charcoal #2D2D2D outlines. Flat comparison chart and small technical schematic, geometric sans-serif body, bilingual labels. No gradients, photographs, glossy effects, logos, slide numbers, footer, or watermark. Exact text only.

Slide type: Content. Narrative goal: show that retrieval quality depends on term weighting before SVD.

Exact text:
- Headline: 词项权重决定哪些共现关系更值得相信
- Sub-headline: LogEntropy 在 CRAN 实验中带来约 40% 平均优势
- Bullet 1: raw term frequency 让所有出现次数直接进入矩阵。
- Bullet 2: Idf、Entropy、LogEntropy 等全局权重降低高频常见词的影响。
- Bullet 3: CRAN 结果中 LogEntropy 表现最好；Normal 和 GfIdf 低于无权重方案。
- Interpretation: 语义空间的质量，先受输入矩阵的质量约束。
- Methods: Raw TF; Normal; GfIdf; Idf; Entropy; LogEntropy
- Source: Dumais (1991), Figure 3 and reported average improvement.

Composition: comparison-grid. Left side shows a ranked qualitative horizontal chart of six methods, with LogEntropy highlighted in teal and no invented numeric bar values. Right side shows a small term-frequency distribution becoming weighted, with common terms fading and informative terms emphasized. Put the 40% annotation beside the LogEntropy row and the interpretation in a maroon outlined panel.
