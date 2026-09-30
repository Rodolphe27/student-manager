package com.student_manager.feature.course;

import com.student_manager.shared.exception.ValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Academic terms ("Winter 2025/26"). Any authenticated user may list them (courses show
 * their term); only an ADMIN may add one, see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermRepository repository;

    /**
     * Lists every term, newest start date first.
     *
     * @return 200 OK with all terms
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<TermDTO>> getAll() {
        return ResponseEntity.ok(repository.findAll(Sort.by(Sort.Direction.DESC, "startDate", "name"))
                .stream().map(TermDTO::of).toList());
    }

    /**
     * Creates a term.
     *
     * @param request the term's name and optional date range
     * @return 201 Created with the new term
     * @throws ValidationException if the name is taken or the end date precedes the start date
     */
    @PostMapping
    @Transactional
    public ResponseEntity<TermDTO> create(@Valid @RequestBody CreateTermRequest request) {
        String name = request.getName().trim();
        if (repository.existsByName(name)) {
            throw new ValidationException("Term already exists: " + name);
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("Term end date must not be before its start date");
        }
        Term term = new Term();
        term.setName(name);
        term.setStartDate(request.getStartDate());
        term.setEndDate(request.getEndDate());
        return ResponseEntity.status(201).body(TermDTO.of(repository.save(term)));
    }
}
