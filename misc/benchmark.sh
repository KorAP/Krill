#!/bin/sh
#
# Benchmark KrillIndex: search response time and total results,
# and the response times of getMatchInfo, numberOf and getFields.
# The report is written to target/krill-benchmark-<timestamp>.txt
# (or --output <file>).
#
#   misc/benchmark.sh                      # in-memory wiki sample, built-in queries
#   misc/benchmark.sh --index /path/to/index \
#       --queries src/test/resources/queries/benchmark1.jsonld --csv bench.csv
#   misc/benchmark.sh --help
#
# Options are passed on to de.ids_mannheim.korap.benchmark.KrillBenchmark
# (arguments must not contain spaces). Set the heap with MAVEN_OPTS,
# e.g. MAVEN_OPTS=-Xmx4g misc/benchmark.sh ...

cd "$(dirname "$0")/.." || exit 1

exec mvn -q test-compile exec:java \
     -Dexec.classpathScope=test \
     -Dexec.mainClass=de.ids_mannheim.korap.benchmark.KrillBenchmark \
     -Dexec.args="$*"
