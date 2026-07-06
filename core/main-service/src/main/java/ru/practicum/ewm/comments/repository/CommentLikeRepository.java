package ru.practicum.ewm.comments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.comments.model.CommentLike;

import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    long countByCommentId(Long commentId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);

    void deleteAllByCommentId(Long commentId);

    @Query("""
       SELECT cl.comment.id, COUNT(cl)
       FROM CommentLike cl
       WHERE cl.comment.id IN :commentIds
       GROUP BY cl.comment.id
       """)
    List<Object[]> countLikesForComments(List<Long> commentIds);
}