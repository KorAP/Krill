![Krill](http://nils-diewald.de/temp/krill.png)

A Corpusdata Retrieval Index using Lucene for Look-Ups


## Description

Krill is a [Lucene](https://lucene.apache.org/) based search
engine for large annotated corpora,
used as a backend component of the [KorAP Corpus Analysis](http://korap.ids-mannheim.de/) at the [IDS Mannheim](http://ids-mannheim.de/).

**! This software is in its early stages and not stable yet! Use it on your own risk!**

## Features

Krill is the reference implementation for
[KoralQuery](https://github.com/KorAP/Koral), covering
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

At least Java 7,
[Git](http://git-scm.com/),
[Maven](https://maven.apache.org/).
Further dependencies are resolved using Maven.


## Setup

To fetch the latest version of Krill ...

```
$ git clone https://github.com/KorAP/Krill
$ cd Krill
```

To configure Krill, edit `bin/log4j.properties` and create `bin/krill.properties` using `bin/krill.properties.info` as a template.

To run the test suite ...

```
$ mvn clean test
```


To start the server ...

```
$ mvn compile exec:java
```

## Caveats

Krill operates on tokens and is limited to a single tokenization stream.
Token annotations therefore have to rely on that tokenization,
span annotations have to wrap at least one token.
Punctuations are currently not supported.
The order of results is currently bound to the order of documents in the
index, but this is likely to change.


## Development and License

**Authors**: [Nils Diewald](http://nils-diewald.de/),
	     [Eliza Margaretha](http://www1.ids-mannheim.de/direktion/personal/margaretha.html)

Copyright (c) 2013-2015, [IDS Mannheim](http://ids-mannheim.de/), Germany

Krill is developed as part of the [KorAP](http://korap.ids-mannheim.de/)
Corpus Analysis Platform at the Institute for German Language
([IDS](http://ids-mannheim.de/)),
funded by the
[Leibniz-Gemeinschaft](http://www.leibniz-gemeinschaft.de/en/about-us/leibniz-competition/projekte-2011/2011-funding-line-2/)
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
[DeReKo](http://ids-mannheim.de/kl/projekte/korpora/) project.

**To cite this work, please refer to:**<br>
Bański, Piotr, Joachim Bingel, Nils Diewald, Elena Frick, Michael Hanl, Marc Kupietz, Piotr Pęzik, Carsten Schnober, and Andreas Witt (2013):
*KorAP: the new corpus analysis platform at IDS Mannheim*. In: Z. Vetulani and H. Uszkoreit (eds):
*Human Language Technologies as a Challenge for Computer Science and Linguistics.*
Proceedings of the 6th Language and Technology Conference. Poznań: Fundacja Uniwersytetu im. A. Mickiewicza. 


## Contributions

Contributions to Krill are very welcome!
Before contributing, please reformat your code changes according to the KorAP
style guideline, provided by means of an
[Eclipse style sheet](https://raw.githubusercontent.com/KorAP/Krill/master/Format.xml).
You can either reformat using [Eclipse](http://eclipse.org/) or (recommended) using
[Maven](https://maven.apache.org/) with the command

```
  $ mvn java-formatter:format
```

Please note that unless you explicitly state otherwise any
contribution intentionally submitted for inclusion into Krill shall –
as Krill itself – be under the [BSD-2 License](https://raw.githubusercontent.com/KorAP/Krill/master/LICENSE).

## References

Annotation tools and models used in preparation of the test corpora are based on the following work:

Bohnet, Bernd (2010): *Top accuracy and fast dependency parsing is not a contradiction*. In *Proceedings of COLING*, pp 89–97, Beijing, China.

Bohnet, Bernd, Joakim Nivre, Igor Boguslavsky, Richard Farkas, Filip Ginter, and Jan Hajic (2013): *Joint Morphological and Syntactic Analysis for Richly Inflected Languages*. Transactions of the Association for Computational Linguistics, 1, pp. 415-428.        

Faruqui, Manaal and Sebastian Padó (2010): *Training and Evaluating a German Named Entity Recognizer with Semantic Generalization*. In *Proceedings of KONVENS 2010*, Saarbrücken, Germany.

Finkel, Jenny R. and Christopher D. Manning (2009): *Joint parsing and named entity recognition*. In *Proceedings of Human Language Technologies: The 2009 Annual Conference of the North American Chapter of the Association for Computational Linguistics, NAACL ’09*, pp. 326–334, Stroudsburg, PA, USA.

Hockenmaier, Julia, Gann Bierner, and Jason Baldridge (2000): *Providing Robustness for a CCG system*. In *Proceedings of the ESSLLI Workshop on Linguistic Theory and Grammar Implementation*, Birmingham, United Kingdom.

Manning, Christopher D., Mihai Surdeanu, John Bauer, Jenny Finkel, Steven J. Bethard, and David McClosky (2014): *The Stanford CoreNLP Natural Language Processing Toolkit*. In *Proceedings of 52nd Annual Meeting of the Association for Computational Linguistics: System Demonstrations*, pp. 55-60.

Schmid, Helmut (1995): *Improvements in Part-of-Speech Tagging with an Application to German*. In *Proceedings of the ACL SIGDAT-Workshop*. Dublin, Ireland.

Schmid, Helmut (1994): *Probabilistic Part-of-Speech Tagging Using Decision Trees*. In *Proceedings of International Conference on New Methods in Language Processing*. Manchester, United Kingdom.
