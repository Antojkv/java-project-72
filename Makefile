.DEFAULT_GOAL := build-run

setup:
	cd app && ./gradlew wrapper --gradle-version 9.2.1

clean:
	cd app && ./gradlew clean

build:
	cd app && ./gradlew clean build test checkstyleMain checkstyleTest

install:
	cd app && ./gradlew clean install

run-dist:
	cd app && ./build/install/app/bin/app $(ARGS)

run:
	cd app && ./gradlew run --args="$(ARGS)"

test:
	cd app && ./gradlew test

report:
	cd app && ./gradlew jacocoTestReport

lint:
	cd app && ./gradlew spotlessApply

update-deps:
	cd app && ./gradlew refreshVersions
	# ./gradlew dependencyUpdates -Drevision=release


build-run: build run

.PHONY: build
