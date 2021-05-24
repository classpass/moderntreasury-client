.PHONY: help build test
DEFAULT_GOAL := help
GRADLE:=$(shell which ./gradlew)

#
# Standard Targets
#

help:
	@echo "build:   Compiles the code."
	@echo "test:    Runs tests."

build: gradle-assemble
test: gradle-check

#
# Internal Targets
#

gradle-assemble:
	$(GRADLE) assemble

gradle-%:
	$(GRADLE) $*
