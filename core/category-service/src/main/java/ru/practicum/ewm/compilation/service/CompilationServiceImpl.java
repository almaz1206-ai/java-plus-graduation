package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.OffsetPageRequest;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.event.EventContract;
import ru.practicum.interaction.event.EventSummaryResponse;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventContract eventContract;

    @Override
    @Transactional
    public CompilationDto saveCompilation(NewCompilationDto dto) {
        log.info("Saving compilation: title={}", dto.getTitle());
        var eventMap = eventMap(dto.getEvents());
        Compilation compilation = new Compilation();
        compilation.setEventIds(new HashSet<>(eventMap.keySet()));
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        compilation.setTitle(dto.getTitle());
        return CompilationMapper.toCompilationDto(compilationRepository.save(compilation), eventMap);
    }

    @Override
    @Transactional
    public void deleteCompilation(long compId) {
        log.info("Deleting compilation id={}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest request) {
        log.info("Updating compilation id={}", compId);
        Compilation compilation = getOrThrow(compId);

        Map<Long, EventSummaryResponse> eventMap = null;
        if (request.getEvents() != null) {
            eventMap = eventMap(request.getEvents());
            compilation.setEventIds(new HashSet<>(eventMap.keySet()));
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        Compilation saved = compilationRepository.save(compilation);
        return eventMap == null ? toDto(saved) : CompilationMapper.toCompilationDto(saved, eventMap);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Getting compilations: pinned={}, from={}, size={}", pinned, from, size);
        Pageable pageable = new OffsetPageRequest(from, size);

        List<Compilation> compilations = pinned != null ?
                compilationRepository.findWithEventsByPinned(pinned, pageable) :
                compilationRepository.findAllWithEvents(pageable);
        Set<Long> ids = compilations.stream().flatMap(c -> c.getEventIds().stream()).collect(Collectors.toSet());
        var events = eventContract.getByIds(new IdsRequest(ids)).events().stream()
                .collect(Collectors.toMap(EventSummaryResponse::id, e -> e));
        return compilations.stream().map(c -> CompilationMapper.toCompilationDto(c, events)).toList();
    }

    @Override
    public CompilationDto getCompilation(long compId) {
        log.info("Getting compilation id={}", compId);
        Compilation compilation = getOrThrow(compId);
        var events = eventContract.getByIds(new IdsRequest(compilation.getEventIds())).events().stream()
                .collect(Collectors.toMap(EventSummaryResponse::id, e -> e));
        return CompilationMapper.toCompilationDto(compilation, events);
    }

    private Compilation getOrThrow(long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private Map<Long, EventSummaryResponse> eventMap(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return eventContract.getByIds(new IdsRequest(ids)).events().stream()
                .collect(Collectors.toMap(EventSummaryResponse::id, e -> e));
    }

    private CompilationDto toDto(Compilation compilation) {
        var events = eventContract.getByIds(new IdsRequest(compilation.getEventIds())).events().stream()
                .collect(Collectors.toMap(EventSummaryResponse::id, e -> e));
        return CompilationMapper.toCompilationDto(compilation, events);
    }
}


