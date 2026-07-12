package ru.practicum.ewm.stats.analyzer.repository;

public interface CandidateNeighborView {

    Long getCandidateId();

    Long getNeighborId();

    Double getUserWeight();

    Double getSimilarity();
}
