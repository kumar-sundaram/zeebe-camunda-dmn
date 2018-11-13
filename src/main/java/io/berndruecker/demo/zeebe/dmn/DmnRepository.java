package io.berndruecker.demo.zeebe.dmn;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmnRepository {
  private static final Logger LOG = LoggerFactory.getLogger(DmnRepository.class);

  private final String directory;
  private final DmnEngine dmnEngine;

  private final Map<String, DmnDecision> decisionsById = new HashMap<>();

  public DmnRepository(String directory, DmnEngine dmnEngine) {
    this.directory = directory;
    this.dmnEngine = dmnEngine;

    scanDirectory();
  }

  public DmnDecision findDecisionById(String decisionId) {
    if (!decisionsById.containsKey(decisionId)) {
      scanDirectory();
    }

    return decisionsById.get(decisionId);
  }

  private void scanDirectory() {
    LOG.debug("Scan directory: {}", directory);

    try {
      Files.walk(Paths.get(directory)).filter(isDmnFile()).forEach(this::readDmnFile);
    } catch (IOException e) {
      LOG.warn("Fail to scan directory: {}", directory, e);
    }
  }

  private void readDmnFile(Path dmnFile) {
    final String fileName = dmnFile.getFileName().toString();

    try {
      final DmnModelInstance dmnModel = Dmn.readModelFromFile(dmnFile.toFile());

      dmnEngine.parseDecisions(dmnModel).forEach(decision -> {
        LOG.debug("Found decision with id '{}' in file: {}", decision.getKey(), fileName);

        decisionsById.put(decision.getKey(), decision);
      });
    } catch (Throwable t) {
      LOG.warn("Failed to parse decision: {}", fileName, t);
    }
  }

  private Predicate<Path> isDmnFile() {
    return p -> p.getFileName().toString().endsWith(".dmn");
  }

}