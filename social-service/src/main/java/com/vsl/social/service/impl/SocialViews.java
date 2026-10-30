package com.vsl.social.service.impl;

import com.vsl.common.response.PageResponse;
import com.vsl.social.client.IdentityClient;
import com.vsl.social.dto.BlogDetailResponse;
import com.vsl.social.dto.BlogResponse;
import com.vsl.social.dto.CommentResponse;
import com.vsl.social.dto.NotificationResponse;
import com.vsl.social.dto.ReplyResponse;
import com.vsl.social.dto.ReportDetailResponse;
import com.vsl.social.dto.ReportResponse;
import com.vsl.social.dto.SocialUserResponse;
import com.vsl.social.dto.UserBriefResponse;
import com.vsl.social.dto.remote.RemoteDtos.UserSummary;
import com.vsl.social.entity.Blog;
import com.vsl.social.entity.BlogComment;
import com.vsl.social.entity.BlogNotification;
import com.vsl.social.entity.BlogReport;
import com.vsl.social.entity.BlogStatus;
import com.vsl.social.entity.CommentReply;
import com.vsl.social.repository.BlogLikeRepository;
import com.vsl.social.repository.BlogRepository;
import com.vsl.social.repository.CommentLikeRepository;
import com.vsl.social.repository.CommentReplyRepository;
import com.vsl.social.repository.ReplyLikeRepository;
import com.vsl.social.repository.UserFollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ghép dữ liệu hiển thị cho response: tên/avatar (identity-service), likedByMe, quan hệ theo dõi, số đếm.
 * Mọi thứ lấy THEO LÔ cho cả trang (1 lần gọi identity, vài query gom nhóm) - monolith chạy 3-5 query
 * cho từng bài / từng bình luận trong danh sách.
 */
@Component
@RequiredArgsConstructor
public class SocialViews {

    private final IdentityClient identityClient;
    private final BlogRepository blogRepository;
    private final BlogLikeRepository blogLikeRepository;
    private final UserFollowRepository followRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final CommentReplyRepository replyRepository;

    // ---------------- Bài viết ----------------

    public PageResponse<BlogResponse> blogPage(Page<Blog> page, Long viewerId) {
        return PageResponse.of(blogs(page.getContent(), viewerId), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public List<BlogResponse> blogs(List<Blog> blogs, Long viewerId) {
        if (blogs.isEmpty()) {
            return List.of();
        }
        BlogContext ctx = blogContext(blogs, viewerId);
        return blogs.stream().map(b -> new BlogResponse(
                b.getId(), ctx.author(b), b.getTitle(), BlogResponse.excerpt(b.getContent()), b.getThumbnailUrl(),
                b.getStatus(), ctx.deletionReason(b), b.getLikeCount(), b.getCommentCount(), b.getShareCount(),
                ctx.liked.contains(b.getId()), ctx.followed(b), ctx.friend(b),
                b.getCreatedAt(), b.getUpdatedAt())).toList();
    }

    public BlogDetailResponse blogDetail(Blog b, Long viewerId) {
        BlogContext ctx = blogContext(List.of(b), viewerId);
        return new BlogDetailResponse(
                b.getId(), ctx.author(b), b.getTitle(), b.getContent(), b.getThumbnailUrl(),
                b.getStatus(), ctx.deletionReason(b), b.getLikeCount(), b.getCommentCount(), b.getShareCount(),
                ctx.liked.contains(b.getId()), ctx.followed(b), ctx.friend(b),
                b.getCreatedAt(), b.getUpdatedAt());
    }

    private BlogContext blogContext(List<Blog> blogs, Long viewerId) {
        Set<Long> blogIds = blogs.stream().map(Blog::getId).collect(Collectors.toSet());
        Set<Long> authorIds = blogs.stream().map(Blog::getAuthorId).collect(Collectors.toSet());
        Set<Long> liked = new HashSet<>();
        Set<Long> followedAuthors = new HashSet<>();
        Set<Long> authorsFollowingViewer = new HashSet<>();
        if (viewerId != null) {
            liked.addAll(blogLikeRepository.findLikedBlogIds(viewerId, blogIds));
            followedAuthors.addAll(followRepository.findFolloweeIdsAmong(viewerId, authorIds));
            authorsFollowingViewer.addAll(followRepository.findFollowerIdsAmong(viewerId, authorIds));
        }
        return new BlogContext(viewerId, AccessControl.isAdmin(), users(authorIds), liked,
                followedAuthors, authorsFollowingViewer);
    }

    private record BlogContext(Long viewerId, boolean admin, Map<Long, UserSummary> authors, Set<Long> liked,
                               Set<Long> followedAuthors, Set<Long> authorsFollowingViewer) {

        UserBriefResponse author(Blog b) {
            return brief(b.getAuthorId(), authors);
        }

        boolean followed(Blog b) {
            return !Objects.equals(b.getAuthorId(), viewerId) && followedAuthors.contains(b.getAuthorId());
        }

        boolean friend(Blog b) {
            return followed(b) && authorsFollowingViewer.contains(b.getAuthorId());
        }

        /** Lý do gỡ chỉ cho tác giả và admin xem. */
        String deletionReason(Blog b) {
            boolean canSee = admin || Objects.equals(b.getAuthorId(), viewerId);
            return b.getStatus() == BlogStatus.HIDDEN && canSee ? b.getDeletionReason() : null;
        }
    }

    // ---------------- Người dùng ----------------

    public Map<Long, UserSummary> users(Collection<Long> ids) {
        return identityClient.getSummaries(ids);
    }

    public UserBriefResponse user(Long id) {
        return id == null ? null : brief(id, users(List.of(id)));
    }

    /** Danh sách người dùng (giữ thứ tự ids) kèm số liệu theo dõi + quan hệ với người xem. */
    public List<SocialUserResponse> socialUsers(List<Long> ids, Map<Long, UserSummary> known, Long viewerId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, UserSummary> summaries = new HashMap<>(known);
        List<Long> missing = ids.stream().filter(id -> !summaries.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            summaries.putAll(users(missing));
        }
        Map<Long, Long> followers = toCountMap(followRepository.countFollowersByIds(ids));
        Map<Long, Long> following = toCountMap(followRepository.countFollowingByIds(ids));
        Set<Long> followedByMe = viewerId == null ? Set.of() : new HashSet<>(followRepository.findFolloweeIdsAmong(viewerId, ids));
        Set<Long> followsMe = viewerId == null ? Set.of() : new HashSet<>(followRepository.findFollowerIdsAmong(viewerId, ids));
        return ids.stream().map(id -> {
            UserSummary s = summaries.get(id);
            boolean byMe = !Objects.equals(id, viewerId) && followedByMe.contains(id);
            boolean me = !Objects.equals(id, viewerId) && followsMe.contains(id);
            return new SocialUserResponse(id,
                    s != null ? s.username() : null, s != null ? s.fullName() : null,
                    s != null ? s.avatarUrl() : null, s != null ? s.displayName() : null,
                    followers.getOrDefault(id, 0L), following.getOrDefault(id, 0L),
                    followRepository.countFriends(id), byMe, me, byMe && me);
        }).toList();
    }

    // ---------------- Bình luận / trả lời ----------------

    public List<CommentResponse> comments(List<BlogComment> comments, Long viewerId) {
        if (comments.isEmpty()) {
            return List.of();
        }
        List<Long> ids = comments.stream().map(BlogComment::getId).toList();
        Map<Long, Long> likeCounts = toCountMap(commentLikeRepository.countByCommentIds(ids));
        Map<Long, Long> replyCounts = toCountMap(replyRepository.countByCommentIds(ids));
        Set<Long> liked = viewerId == null ? Set.of() : new HashSet<>(commentLikeRepository.findLikedCommentIds(viewerId, ids));
        Map<Long, UserSummary> users = users(comments.stream()
                .flatMap(c -> Stream.of(c.getUserId(), c.getMentionedUserId())).filter(Objects::nonNull).toList());
        return comments.stream().map(c -> new CommentResponse(
                c.getId(), c.getBlog().getId(),
                brief(c.getUserId(), users),
                brief(c.getMentionedUserId(), users),
                c.getContent(), replyCounts.getOrDefault(c.getId(), 0L), likeCounts.getOrDefault(c.getId(), 0L),
                liked.contains(c.getId()), c.getCreatedAt(), c.getUpdatedAt())).toList();
    }

    public List<ReplyResponse> replies(List<CommentReply> replies, Long viewerId) {
        if (replies.isEmpty()) {
            return List.of();
        }
        List<Long> ids = replies.stream().map(CommentReply::getId).toList();
        Map<Long, Long> likeCounts = toCountMap(replyLikeRepository.countByReplyIds(ids));
        Set<Long> liked = viewerId == null ? Set.of() : new HashSet<>(replyLikeRepository.findLikedReplyIds(viewerId, ids));
        Map<Long, UserSummary> users = users(replies.stream()
                .flatMap(r -> Stream.of(r.getUserId(), r.getMentionedUserId())).filter(Objects::nonNull).toList());
        return replies.stream().map(r -> new ReplyResponse(
                r.getId(), r.getComment().getId(),
                brief(r.getUserId(), users),
                brief(r.getMentionedUserId(), users),
                r.getContent(), likeCounts.getOrDefault(r.getId(), 0L), liked.contains(r.getId()),
                r.getCreatedAt(), r.getUpdatedAt())).toList();
    }

    // ---------------- Thông báo ----------------

    public List<NotificationResponse> notifications(List<BlogNotification> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, UserSummary> actors = users(list.stream().map(BlogNotification::getActorId).toList());
        Set<Long> blogIds = list.stream().map(BlogNotification::getBlogId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> titles = blogRepository.findAllById(blogIds).stream()
                .collect(Collectors.toMap(Blog::getId, Blog::getTitle));
        return list.stream().map(n -> new NotificationResponse(
                n.getId(), n.getType(), brief(n.getActorId(), actors),
                n.getBlogId(), n.getBlogId() != null ? titles.get(n.getBlogId()) : null,
                n.getCommentId(), n.getReplyId(), n.isRead(), n.getCreatedAt())).toList();
    }

    // ---------------- Báo cáo vi phạm ----------------

    public List<ReportResponse> reports(List<BlogReport> reports) {
        Map<Long, UserSummary> users = users(reports.stream().map(BlogReport::getReporterId).toList());
        return reports.stream().map(r -> new ReportResponse(
                r.getId(), r.getBlog().getId(), r.getBlog().getTitle(), r.getBlog().getStatus(),
                brief(r.getReporterId(), users),
                r.getReason(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt())).toList();
    }

    public ReportDetailResponse reportDetail(BlogReport r) {
        Blog b = r.getBlog();
        Map<Long, UserSummary> users = users(List.of(r.getReporterId(), b.getAuthorId()));
        return new ReportDetailResponse(
                r.getId(), b.getId(), b.getTitle(), b.getContent(), b.getThumbnailUrl(), b.getStatus(),
                b.getDeletionReason(),
                brief(b.getAuthorId(), users),
                brief(r.getReporterId(), users),
                r.getReason(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt());
    }

    /** Không gọi map.get(null) (Map bất biến ném NPE với khóa null). */
    private static UserBriefResponse brief(Long userId, Map<Long, UserSummary> users) {
        return userId == null ? null : UserBriefResponse.of(userId, users.get(userId));
    }

    static <E, T> PageResponse<T> page(Page<E> page, Function<List<E>, List<T>> mapper) {
        return PageResponse.of(mapper.apply(page.getContent()), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return map;
    }
}
