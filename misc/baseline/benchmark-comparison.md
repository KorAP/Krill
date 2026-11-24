# Krill benchmark: Lucene 5.0.0, 9.12.3 and 10.5.1

KrillBenchmark on the in-memory wiki sample (10 documents, 1 segment), with the built-in queries and the default settings (3 cold rounds, 5 warm-up rounds, 20 warm rounds), on Java 25.0.2 with a 2048 MB heap. Each column is one run; repeated runs of the same version differ by up to about 20 %.

- 5.0.0: before the upgrade (b20cd459)
- 9.12.3: last commit on Lucene 9 (59f148d4)
- 10.5.1: after the upgrade (d3aba859)

Reports: `benchmark-lucene-<version>-wiki.txt` and `.csv` in this directory.

## Summary

- All searches find the same results and texts in all three versions.
- Seven of the nine searches take 0.25 to 1.1 ms longer on 10.5.1 than on 5.0.0 (up to 48 %, for `annotation`, `segment` and `regex`); `class` and `startswith` are as fast as on 5.0.0. This comes from the in-memory index of the benchmark, not from Lucene's search (see below).
- Match info is as fast as or faster than on 5.0.0, except for snippets only (0.031 to 0.043 ms).
- Statistics and metadata fields are about as fast as on 5.0.0.

## Search

Warm median in ms; results and texts are the same in all three versions.

| Query | Results | Texts | 5.0.0 | 9.12.3 | 10.5.1 |
|---|---:|---:|---:|---:|---:|
| `term [s:der]` | 207 | 10 | 1.991 | 2.472 | 2.654 |
| `annotation [mate/m:gender:masc]` | 497 | 7 | 2.240 | 2.787 | 3.307 |
| `segment [tt/p:NN & mate/m:number:pl]` | 120 | 7 | 2.077 | 2.571 | 3.033 |
| `regex [s:.*ung]` | 69 | 8 | 2.466 | 2.970 | 3.509 |
| `sequence [tt/p:ART][tt/p:ADJA][tt/p:NN]` | 127 | 9 | 2.266 | 2.496 | 2.552 |
| `repetition [tt/p:ADJA]{2,3}[tt/p:NN]` | 29 | 8 | 2.416 | 2.749 | 2.847 |
| `class {1:[tt/p:ADJA]}[s:Sprache]` | 2 | 2 | 0.863 | 0.963 | 0.785 |
| `contains(<base/s:s>, [tt/p:NE][tt/p:NE])` | 133 | 9 | 2.182 | 2.299 | 2.429 |
| `startswith(<base/s:s>, [tt/p:ART])` | 43 | 8 | 1.652 | 1.603 | 1.618 |

## Match info

Median in ms over the matches of all queries (42 matches, 840 calls).

| Request | 5.0.0 | 9.12.3 | 10.5.1 |
|---|---:|---:|---:|
| snippet only | 0.031 | 0.032 | 0.043 |
| tokens: all annotations | 0.483 | 0.963 | 0.314 |
| tokens+spans: all annotations | 0.348 | 0.332 | 0.326 |
| tokens+spans, sentence: all annotations | 0.235 | 0.268 | 0.139 |

## Statistics and metadata fields

Warm median in ms.

| Request | 5.0.0 | 9.12.3 | 10.5.1 |
|---|---:|---:|---:|
| `numberOf("documents")` | 0.003 | 0.002 | 0.002 |
| `getFields(id)` | 0.132 | 0.142 | 0.156 |
| `getFields(id, [textSigle,title,author])` | 0.079 | 0.075 | 0.087 |

## Why searches are slower in memory

The benchmark builds its index in memory. eac39e94 (still on Lucene 7.7.3) replaced `RAMDirectory`, which Lucene 9 removes, with `ByteBuffersDirectory`, and searches on that directory are slower. Sums of the warm medians of the nine searches, without snippets, 50 rounds, median of four runs that alternated between the variants:

| Variant | Sum in ms |
|---|---:|
| 7.7.3, `RAMDirectory` (331c7fd5) | 16.24 |
| 7.7.3, `ByteBuffersDirectory` (eac39e94) | 18.30 |
| eac39e94 with `RAMDirectory` again | 16.37 |
| 10.5.1, `ByteBuffersDirectory` (d3aba859) | 17.92 |

Production indexes are opened with `MMapDirectory`. On an on-disk index of the same ten documents, written by the Indexer of each version, 10.5.1 is as fast as 7.7.3, with the same results for every query:

| Variant | Sum in ms |
|---|---:|
| 7.7.3, `MMapDirectory` (331c7fd5) | 20.03 |
| 10.5.1, `MMapDirectory` (d3aba859) | 20.25 |
