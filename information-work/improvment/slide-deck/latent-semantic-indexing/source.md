# Source Digest

## Bibliographic Record

Susan T. Dumais. “Improving the retrieval of information from external sources.” *Behavior Research Methods, Instruments, & Computers*, 1991, 23(2), 229-236. The source is the uploaded file `BF03203370.pdf`.

## Problem

External information retrieval suffers from a vocabulary mismatch. Different authors may describe the same idea with different words (synonymy), while one word may refer to multiple things (polysemy). Exact lexical matching therefore misses relevant documents and returns irrelevant ones. The paper cites a legal retrieval study in which two lawyers found only 20% of known relevant material after searching a 40,000-document full-text collection.

## Proposed Method: Latent Semantic Indexing

LSI starts with a term-by-document association matrix. Singular value decomposition (SVD) decomposes the matrix and retains only the largest `k` singular values and corresponding vectors. The reduced representation removes some noise while preserving major co-occurrence structure. Terms, documents, and queries are represented in the same `k`-dimensional space and compared using cosine similarity.

The practical implication is that documents can be near a query even when they share no exact terms, provided their broader usage patterns are related. The factors are not required to have verbal interpretations; they are used as a computational representation of association structure.

## Evaluation Setup

The paper evaluates LSI on MED, CISI, CRAN, TIME, and ADI collections using bibliographic abstracts or short articles, user queries, and relevance judgments. Performance is summarized with precision and recall. The comparison baseline is automatically indexed word-based retrieval.

| Collection | Documents | Queries | Avg. relevant documents/query |
|---|---:|---:|---:|
| MED | 1,033 | 30 | 23 |
| CISI | 1,460 | 35 | 50 |
| CRAN | 1,400 | 225 | 8 |
| TIME | 425 | 83 | 4 |
| ADI | 82 | 35 | 5 |

## Reported Results

- A 90-dimensional LSI representation on MED improves discrimination of relevant from irrelevant documents by roughly 30% relative to word matching.
- Across collections, LSI averages about 20% better performance, ranging from comparable to about 30% better than standard vector methods.
- In the MED dimensionality experiment, performance rises sharply after roughly 10-20 dimensions, peaks around 70-100 dimensions, and then declines slowly toward the word-matching level.
- Differential term weighting matters. In the CRAN experiment, LogEntropy performs best and yields about a 40% average advantage over raw term frequency.
- Relevance feedback improves performance by about 33% when the first relevant document is used and about 67% when the first three relevant documents are used. The first three relevant documents are found after a median of seven documents; the first relevant document after a median of one.
- The paper notes that LSI remains imperfect, especially at high recall, and that the appropriate number of dimensions depends on collection breadth.

## Independent Reading Notes

1. LSI is best understood as a controlled loss of lexical detail that trades exact wording for robust association patterns.
2. The strongest gains come from a system design stack: representation (LSI), weighting (LogEntropy), and interaction (relevance feedback). Treating LSI as a standalone algorithm understates the result.
3. Relevance feedback is not merely a ranking trick; it externalizes part of the user's intent and turns retrieval into an iterative sensemaking process.
4. The paper's evaluation emphasizes average precision-recall behavior. A modern reading should also ask which queries fail, how much user effort is required, and whether the latent space is interpretable or fair for a given collection.
5. The method predates neural embeddings but anticipates a durable principle: retrieval quality depends on representing relationships among terms and documents, not only on exact token overlap.

## Citation Notes

Key cited works in the paper include Deerwester et al. (1990) on indexing by latent semantic analysis, Furnas et al. (1987) on the vocabulary problem, Blair and Maron (1985) on legal retrieval effectiveness, Sparck Jones (1972) on term specificity, and Salton and Buckley (1990) on relevance feedback.
