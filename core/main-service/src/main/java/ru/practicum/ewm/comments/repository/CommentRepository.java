package ru.practicum.ewm.comments.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.comments.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(value = """
            SELECT c
            FROM Comment c
            LEFT JOIN CommentLike cl ON cl.comment = c
            WHERE c.event.id = :eventId
            GROUP BY c
            ORDER BY COUNT(cl) DESC
            """, countQuery = "SELECT COUNT(DISTINCT c) FROM Comment c WHERE c.event.id = :eventId")
    Page<Comment> findAllByEventIdOrderByLikesDesc(Long eventId, Pageable pageable);

    @Query(value = """
            SELECT c
            FROM Comment c
            LEFT JOIN CommentLike cl ON cl.comment = c
            WHERE c.author.id = :userId
            GROUP BY c
            ORDER BY COUNT(cl) DESC
            """, countQuery = "SELECT COUNT(DISTINCT c) FROM Comment c WHERE c.author.id = :userId")
    Page<Comment> findAllByAuthorIdOrderByLikesDesc(Long userId, Pageable pageable);

    @Query(value = """
            SELECT c
            FROM Comment c
            LEFT JOIN CommentLike cl ON cl.comment = c
            WHERE c.event.id = :eventId
            GROUP BY c
            ORDER BY COUNT(cl) ASC
            """, countQuery = "SELECT COUNT(DISTINCT c) FROM Comment c WHERE c.event.id = :eventId")
    Page<Comment> findAllByEventIdOrderByLikesAsc(Long eventId, Pageable pageable);

    @Query(value = """
            SELECT c
            FROM Comment c
            LEFT JOIN CommentLike cl ON cl.comment = c
            WHERE c.author.id = :userId
            GROUP BY c
            ORDER BY COUNT(cl) ASC
            """, countQuery = "SELECT COUNT(DISTINCT c) FROM Comment c WHERE c.author.id = :userId")
    Page<Comment> findAllByAuthorIdOrderByLikesAsc(Long userId, Pageable pageable);


}
