![Krill](https://raw.githubusercontent.com/KorAP/Krill/master/misc/krill.png)

A Corpusdata Retrieval Index using Lucene for Look-Ups


## Description

Krill is a [Lucene](https://lucene.apache.org/) based search
engine for large annotated corpora,
used as a backend component of the [KorAP Corpus Analysis Platform](https://korap.ids-mannheim.de/) at the [IDS Mannheim](https://www.ids-mannheim.de/).

## Features

Krill is the reference implementation for
[KoralQuery](https://korap.github.io/Koral), covering
most of the protocols features, including ...

- **Fulltext search**<br>
  *"Find all occurrences of the phrase 'sea monster'!"*<br>
  *"Find all case-insensitive words matching the regular expression /krak.*/!"*

- **Token-based annotation search**<br>
  *"Find all plural nouns in accusative!"*

- **Span-based annotation search**<br>
  *"Find all nominal phrases!"*

- **Distance search**<br>
  *"Find a verb that is in a distance of five words to the noun 'Squid'!"*

- **Positional search**<br>
  *"Find a noun at the end of a nominal phrase!"*

- **Nested queries**<br>
  *"Find a determiner at the beginning of a named entity, that occurs at the end of a sentence!"*

- **and many more ...**<br>
  Multiple annotation resources;
  Virtual Collections;
  Partial highlightings;
  Support for overlapping spans;
  Relational queries;
  Hierarchical queries ...

## Prerequisites

Java 21 (OpenJDK or Oracle JDK),
[Git](http://git-scm.com/),
[Maven 3](https://maven.apache.org/).
Further dependencies are resolved using Maven.


## Setup

To install the latest version of Krill from scratch, do ...

```
git clone https://github.com/KorAP/Krill
cd Krill
```

Then run the test suite ...

```
mvn clean test
```

To build a Krill library and install it in your local Maven repository
(needed for Kustvakt) ...

```
mvn install
```

## Command-line Tools

Besides running as a search server, Krill ships two standalone
command-line tools, both packaged as executable jars by `mvn package`.

### Indexer

The `Indexer` reads corpus documents and adds/updates them in a Krill
(Lucene) index.

```
java -jar Krill-Indexer.jar -c <properties file> \
     [-i <input paths>] [-D <field> <value>] \
     [-o <output directory>] [-a] [--progress]
```

- `-c`, `--config <properties file>` — configuration file (required),
  see `src/main/resources/krill.properties.info`.
- `-i`, `--input <input paths>` — semicolon-separated list of input
  paths. Each path can be a directory of `*.json.gz` files, a zip
  file, or a tar file (including `.tar.gz`/`.tgz`), containing
  `.json` or `.json.gz` documents; the type is auto-detected.
- `-o`, `--outputDir <directory>` — index output directory (defaults
  to `krill.indexDir` from the configuration).
- `-D`, `--delete <field> <value>` — delete documents matching
  `field=value` instead of indexing (example: `-D textSigle
  GOE/AGX/00002`).
- `-a`, `--addInsteadofUpsert` — always add documents, never update
  existing ones (upsert is the default).
- `--progress` — show a progress bar with ETA while indexing.

Documents are upserted by default, so an interrupted run can simply
be restarted with the same input paths.

### Metadata Exporter

The `MetaExporter` dumps the per-document metadata stored in an
existing index as TSV, CSV or line-delimited JSON, without going
through the search API.

```
java -jar Krill-MetaExporter.jar -i <index dir> \
     [-f <field,field,...>] [--format tsv|csv|json] \
     [-o <file>] [--list-fields]
```

- `-i`, `--index <index dir>` — index directory to read (required).
- `-f`, `--fields <field,field,...>` — metadata fields/columns to
  export (defaults to all stored fields).
- `--format tsv|csv|json` — output format (default `tsv`).
- `-o`, `--output <file>` — output file (defaults to stdout).
- `--no-header` — omit the header row (TSV/CSV only).
- `--multi-sep <sep>` — separator for multi-valued fields in TSV/CSV
  output (default `|`).
- `--list-fields` — only list the metadata field names present in the
  index and exit.

Example: dump the text sigles of two index instances and diff the
sorted lists to find documents missing from / surplus in one of them:

```
java -jar Krill-MetaExporter.jar -i /path/to/indexA -f textSigle > a.txt
java -jar Krill-MetaExporter.jar -i /path/to/indexB -f textSigle > b.txt
diff <(sort a.txt) <(sort b.txt)
```

### Checking and Repairing an Index

Krill indices are plain Lucene indices, so Lucene's bundled
`CheckIndex` tool (included in the shaded jars) can be used to verify
or repair one directly, without any Krill-specific tooling:

```
java -cp Krill-Server.jar org.apache.lucene.index.CheckIndex /path/to/indexDir
```

This reports the state of every segment without changing anything.
Only if it reports actual corruption, re-run with `-exorcise` to drop
the broken segments and make the index consistent again; note that
this permanently discards the documents in those segments, so prefer
restoring from a backup or re-indexing the affected documents if
possible:

```
java -cp Krill-Server.jar org.apache.lucene.index.CheckIndex -exorcise /path/to/indexDir
```

## Caveats

Krill operates on tokens and is limited to a single tokenization stream.
Token annotations therefore have to rely on that tokenization,
span annotations have to wrap at least one token.
Punctuations are currently not supported.
The order of results is currently bound to the order of documents in the
index, but this is likely to change.


## Development and License

**Authors**: [Nils Diewald](https://www.nils-diewald.de/),
	     [Eliza Margaretha](https://perso.ids-mannheim.de/seiten/margaretha.html)

Copyright (c) 2013-2026, [IDS Mannheim](https://www.ids-mannheim.de/), Germany

Krill is developed as part of the [KorAP](https://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Institute for German Language
([IDS](https://www.ids-mannheim.de/)),
funded by the
[Leibniz-Gemeinschaft](https://www.leibniz-gemeinschaft.de/)
and supported by the [KobRA](http://www.kobra.tu-dortmund.de) project,
funded by the Federal Ministry of Education and Research
([BMBF](http://www.bmbf.de/en/)).

For recent changes and compatibility issues, please consult the
[Changes](https://raw.githubusercontent.com/KorAP/Krill/master/Changes)
file.

Krill is published under the
[BSD-2 License](https://raw.githubusercontent.com/KorAP/Krill/master/LICENSE).
The Eclipse format style is based on the default style in Eclipse,
licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).
Parts of the test corpus by courtesy of the
[DeReKo](https://www.ids-mannheim.de/digspra/kl/projekte/korpora/) project.

**To cite this work, please refer to:**
Diewald/Margaretha (2017).

## Contributions

Contributions to Krill are very welcome!
Before contributing, please reformat your code changes according to the KorAP
style guideline, provided by means of an
[Eclipse style sheet](https://raw.githubusercontent.com/KorAP/Krill/master/Format.xml).
You can either reformat using [Eclipse](http://eclipse.org/) or (recommended) using
[Maven](https://maven.apache.org/) with the command

```
  mvn java-formatter:format
```

Your contributions should ideally be committed via our [Gerrit server](https://korap.ids-mannheim.de/gerrit/)
to facilitate reviewing (see [Gerrit Code Review - A Quick Introduction](https://korap.ids-mannheim.de/gerrit/Documentation/intro-quick.html)
if you are not familiar with Gerrit). However, we are also happy to accept comments and pull requests
via GitHub.

Please note that unless you explicitly state otherwise any
contribution intentionally submitted for inclusion into Krill shall –
as Krill itself – be under the [BSD-2 License](https://raw.githubusercontent.com/KorAP/Krill/master/LICENSE).

## References

Diewald, Nils / Margaretha, Eliza (2017): ‎[Krill: KorAP search and analysis engine](https://doi.org/10.21248/jlcl.31.2016.202).
In: Journal for Language Technology and Computational Linguistics (JLCL), 31 (1), pp. 63-80.

Kupietz, Marc / Diewald, Nils / Lüngen, Harald / Margaretha Illig, Eliza / Stallkamp, Helge / Tran, Uyen-Nhu / Yaddehige, Rameela (2026): [EuReCo, KorAP and DeReKo: Updates on Ingestion and Annotation Pipelines, Backend, Interfaces, Operation, and Corpora](https://doi.org/10.63317/2xcbb5knp2mi).
In: Proceedings of the 12th Workshop on Challenges in the Management of Large Corpora, pp. 106-112, Palma, Mallorca, Spain: European Language Resources Association (ELRA).
