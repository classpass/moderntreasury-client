.PHONY: help build test
DEFAULT_GOAL := help
GRADLE:=$(shell which ./gradlew)
LIVETESTS := $(if $(shell test -f live-test/src/test/resources/live-tests.properties && echo Ok),-PliveTests,)

#
# Standard Targets
#

help:
	@echo "build:   Compiles the code."
	@echo "test:    Runs tests ($(LIVETESTS))"

build: gradle-assemble
test: gradle-check

#
# Internal Targets
#

gradle-assemble:
	$(GRADLE) assemble

gradle-check:
	$(GRADLE) check $(LIVETESTS)
	$(GRADLE) :client:lintKotlin

gradle-%:
	$(GRADLE) $*
