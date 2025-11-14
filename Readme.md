![Krill](https://raw.githubusercontent.com/KorAP/Krill/master/misc/krill.png)

A Corpusdata Retrieval Index using Lucene for Look-Ups


## Description

Krill is a [Lucene](https://lucene.apache.org/) based search
engine for large annotated corpora,
used as a backend component of the [KorAP Corpus Analysis Platform](https://korap.ids-mannheim.de/) at the [IDS Mannheim](https://www.ids-mannheim.de/).

**! This software is in its early stages and not stable yet! Use it on your own risk!**

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

To update an existing repository, pull the latest version at the Krill
installation directory

```
git pull origin master
```

Afterwards, rerun the test suite and install the library.

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

Copyright (c) 2013-2024, [IDS Mannheim](https://www.ids-mannheim.de/), Germany

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

**To cite this work, please refer to:**<br>
Diewald, Nils and Margaretha, Eliza (2017): ‎[Krill: KorAP search and analysis engine](http://www.jlcl.org/2016_Heft1/jlcl-2016-1-4DiewaldMargaretha.pdf).
In: Journal for Language Technology and Computational Linguistics (JLCL), 31 (1), pp. 63-80.

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

Annotation tools and models used in preparation of the test corpora are based on the following work:

Belica, Cyril (1994): *A German Lemmatizer*. MECOLB Final Report MLAP93-21/WP2. Luxemburg.

Bohnet, Bernd (2010): *Top accuracy and fast dependency parsing is not a contradiction*. In *Proceedings of COLING*, pp 89–97, Beijing, China.

Bohnet, Bernd, Joakim Nivre, Igor Boguslavsky, Richard Farkas, Filip Ginter, and Jan Hajic (2013): *Joint Morphological and Syntactic Analysis for Richly Inflected Languages*. Transactions of the Association for Computational Linguistics, 1, pp. 415-428.        

Faruqui, Manaal and Sebastian Padó (2010): *Training and Evaluating a German Named Entity Recognizer with Semantic Generalization*. In *Proceedings of KONVENS 2010*, Saarbrücken, Germany.

Finkel, Jenny R. and Christopher D. Manning (2009): *Joint parsing and named entity recognition*. In *Proceedings of Human Language Technologies: The 2009 Annual Conference of the North American Chapter of the Association for Computational Linguistics, NAACL ’09*, pp. 326–334, Stroudsburg, PA, USA.

Hockenmaier, Julia, Gann Bierner, and Jason Baldridge (2000): *Providing Robustness for a CCG system*. In *Proceedings of the ESSLLI Workshop on Linguistic Theory and Grammar Implementation*, Birmingham, United Kingdom.

Manning, Christopher D., Mihai Surdeanu, John Bauer, Jenny Finkel, Steven J. Bethard, and David McClosky (2014): *The Stanford CoreNLP Natural Language Processing Toolkit*. In *Proceedings of 52nd Annual Meeting of the Association for Computational Linguistics: System Demonstrations*, pp. 55-60.

Schmid, Helmut (1995): *Improvements in Part-of-Speech Tagging with an Application to German*. In *Proceedings of the ACL SIGDAT-Workshop*. Dublin, Ireland.

Schmid, Helmut (1994): *Probabilistic Part-of-Speech Tagging Using Decision Trees*. In *Proceedings of International Conference on New Methods in Language Processing*. Manchester, United Kingdom.
