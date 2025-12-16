PORT ?=8080
run:
	./gradlew bootRun -Dserver.port=$(PORT)

build:
	./gradlew clean build

test:
	./gradlew test

clean:
	./gradlew clean

.PHONY: run build test clean