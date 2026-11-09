package com.outreach.planner.service;

import com.outreach.planner.dto.PlannerNoteRequest;
import com.outreach.planner.dto.PlannerNoteResponse;
import com.outreach.planner.model.PlannerNote;
import com.outreach.planner.repository.PlannerNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlannerNoteService {

    private final PlannerNoteRepository noteRepository;

    @Transactional(readOnly = true)
    public List<PlannerNoteResponse> list(Long userId) {
        return noteRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(PlannerNoteResponse::from).toList();
    }

    @Transactional
    public PlannerNoteResponse create(Long userId, PlannerNoteRequest request) {
        PlannerNote note = PlannerNote.builder().userId(userId).content(request.content().trim()).build();
        return PlannerNoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public PlannerNoteResponse update(Long userId, Long id, PlannerNoteRequest request) {
        PlannerNote note = find(userId, id);
        note.setContent(request.content().trim());
        return PlannerNoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        noteRepository.delete(find(userId, id));
    }

    private PlannerNote find(Long userId, Long id) {
        return noteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note " + id + " not found"));
    }
}
