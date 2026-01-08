# CLAUDE.md

- Make sure to run any mvn commands with `JAVA_HOME=/opt/homebrew/opt/openjdk`

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ICE (Immunization Calculation Engine) is an open-source clinical decision support system for immunization forecasting and evaluation. It uses the OpenCDS framework and Drools 10 rules engine to provide vaccine recommendations based on CDC schedules and clinical data.

**Key Resources:**
- Project Wiki: https://cdsframework.atlassian.net/wiki/spaces/ICE/overview
- Installation Guide: https://cdsframework.atlassian.net/wiki/spaces/ICE/pages/18972687/Installing+ICE
- Release Notes: https://cdsframework.atlassian.net/wiki/spaces/ICE/pages/78479371/Release+Notes
- News: https://cdsframework.atlassian.net/wiki/spaces/ICE/pages/23920670/News
- Commercial Support: https://www.hln.com/ice/

**Version:** 4.0.1-SNAPSHOT
**OpenCDS Version:** 7.0.1
**Java Version:** 25 (required)
**Drools Version:** 10.1.0
**Runtime:** Tomcat 10 or Tomcat 11 (2GB memory recommended)

## Architecture

### Multi-Module Structure

The project uses a multi-module Maven build with pre-compiled Drools rules for fast startup:

```
ice/
├── pom.xml                     # Parent POM (ice-parent)
├── ice-rules-kjar/             # Pre-compiled Drools rules (KJAR)
│   ├── src/main/resources/
│   │   ├── drools/             # DRL/DSLR rule files
│   │   └── META-INF/kmodule.xml
│   └── pom.xml
├── opencds-ice-service/        # Main ICE service (WAR)
│   ├── src/main/java/          # Application code
│   ├── src/main/resources/
│   │   └── config/supportingData/
│   └── pom.xml
└── local-repo/                 # Local Maven repository for OpenCDS artifacts
```

### Module Details

1. **ice-parent** - Parent POM
   - Manages versions and dependencies for all modules
   - Artifact: `org.cdsframework:ice-parent:4.0.1-SNAPSHOT`

2. **ice-rules-kjar** - Knowledge JAR (KJAR)
   - Contains all Drools DRL/DSLR rule files (67 files)
   - Pre-compiled at build time using kie-maven-plugin
   - Artifact: `org.cdsframework:ice-rules-kjar:4.0.1-SNAPSHOT`
   - Packaging: `kjar`

3. **opencds-ice-service** - Main ICE Service (WAR)
   - ICE-specific implementation of DSS evaluation using Drools 10
   - Loads pre-compiled rules from KJAR at runtime
   - Contains supporting data and configuration
   - Final deployable artifact: `opencds-ice-service.war`

### Key Components

**ICE Core Classes** (`opencds-ice-service/src/main/java/org/cdsframework/ice/service/`):
- `Vaccine`, `VaccineComponent` - Vaccine data models
- `TargetDose`, `DoseRule`, `DoseStatus` - Dose evaluation logic
- `Recommendation`, `RecommendationStatus` - Recommendation output
- `Schedule`, `SeriesRules`, `Season` - Scheduling and series logic
- `ICELogicHelper` - Core helper for ICE business logic

**Configuration Classes** (`opencds-ice-service/src/main/java/org/cdsframework/ice/config/`):
- `OpenCdsConfig` - Main Spring configuration
- `PathConfig` - Resource path configuration
- `IceProperties` - ICE-specific properties

**Knowledge Loading** (`opencds-ice-service/src/main/java/org/cdsframework/ice/service/configurations/`):
- `IceKnowledgeLoader` - Loads pre-compiled KieBase from KJAR
- `ICEDecisionEngineDSSEvaluationAdapter` - Drools execution adapter

**Supporting Data** (`opencds-ice-service/src/main/resources/config/supportingData/`):
- Vaccine concepts, groups, and series defined in XML files
- Configuration loaded via `ICEPropertiesDataConfiguration`

**Drools Rules** (`ice-rules-kjar/src/main/resources/drools/`):
- `knowledgeCommon/` - Shared rule definitions
- `knowledgeModule/` - Vaccine-specific evaluation and recommendation rules

## Build Commands

### Staged Build (Required)

Due to circular dependencies between modules, a staged build is required:

```bash
# Stage 1: Build WAR to generate classes JAR
cd opencds-ice-service
JAVA_HOME=/opt/homebrew/opt/openjdk mvn clean install -DskipTests

# Stage 2: Build KJAR (uses WAR classes for DRL compilation)
cd ../ice-rules-kjar
JAVA_HOME=/opt/homebrew/opt/openjdk mvn clean install -DskipTests

# Stage 3: Rebuild WAR with KJAR included
cd ../opencds-ice-service
JAVA_HOME=/opt/homebrew/opt/openjdk mvn clean install -DskipTests
```

### Quick Rebuild (After Initial Build)

If only changing application code (not rules):
```bash
cd opencds-ice-service
JAVA_HOME=/opt/homebrew/opt/openjdk mvn clean package -DskipTests
```

If only changing rules:
```bash
cd ice-rules-kjar
JAVA_HOME=/opt/homebrew/opt/openjdk mvn clean install -DskipTests
cd ../opencds-ice-service
JAVA_HOME=/opt/homebrew/opt/openjdk mvn clean package -DskipTests
```

### Run Tests

```bash
cd opencds-ice-service

# Unit tests only
JAVA_HOME=/opt/homebrew/opt/openjdk mvn test

# Integration tests
JAVA_HOME=/opt/homebrew/opt/openjdk mvn verify

# Single test
JAVA_HOME=/opt/homebrew/opt/openjdk mvn test -Dtest=YourTestSpec
```

### Run Locally

```bash
cd opencds-ice-service
JAVA_HOME=/opt/homebrew/opt/openjdk mvn spring-boot:run

# With custom port
JAVA_HOME=/opt/homebrew/opt/openjdk mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Deploy

The deployable WAR file is located at:
```
opencds-ice-service/target/opencds-ice-service.war
```

Deploy to Tomcat 10/11:
```bash
cp opencds-ice-service/target/opencds-ice-service.war $CATALINA_HOME/webapps/
```

Or run standalone:
```bash
java -jar opencds-ice-service/target/opencds-ice-service.war
```

## Configuration

### Application Configuration
- `opencds-ice-service/src/main/resources/application.yml` - Spring Boot configuration
- `opencds-ice-service/src/main/resources/ice.properties` - ICE-specific settings

### ICE Properties (ice.properties)
- `output_earliest_and_overdue_dates=Y` - Include earliest/overdue dates in forecasts
- `output_supplemental_text=Y` - Include supplemental text in recommendations
- `enable_dose_override_feature=N` - Allow dose overrides
- `vaccine_group_exclusions` - Comma-separated list of vaccine groups to exclude

### KJAR Configuration
- `ice-rules-kjar/src/main/resources/META-INF/kmodule.xml` - Drools KieBase configuration

## Important Implementation Details

### Pre-compiled Rules (KJAR Architecture)
- Rules are pre-compiled at build time using kie-maven-plugin with executable model
- IceKnowledgeLoader loads KieBase from KJAR via ReleaseId

### VMR (Virtual Medical Record)
ICE uses OpenCDS VMR 1.0 as the input/output data model. Patient data, immunizations, and observations are mapped to VMR before rule evaluation.

### Drools Rules Engine
- Rules are written in Drools DRL format (`.drl` files) and DSLR format (`.dslr` files)
- Rules are pre-compiled into KJAR at build time
- Each vaccine series has corresponding series XML definitions and DRL rules

### REST API Endpoints
- `/evaluate` - Main evaluation endpoint
- `/health` - Health check endpoint
- `/version` - Version information endpoint

### Testing
- Unit tests use Groovy Spock framework (`*Spec.groovy` files)
- Integration tests use the suffix `*FunctionalSpec.groovy`

## Development Workflow

1. Make changes to ICE service code in `opencds-ice-service/src/main/java/`
2. Modify rules in `ice-rules-kjar/src/main/resources/drools/`
3. Update supporting data in `opencds-ice-service/src/main/resources/config/supportingData/`
4. Run staged build (see Build Commands above)
5. Test locally: `mvn spring-boot:run`
6. Run tests: `mvn verify`

## Git Workflow

- **Main branch:** `main-v2`
- Use `main-v2` as the base branch for pull requests
- Current version: 4.0.1-SNAPSHOT
