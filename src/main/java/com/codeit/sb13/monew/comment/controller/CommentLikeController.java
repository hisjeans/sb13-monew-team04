package com.codeit.sb13.monew.comment.controller;

import com.codeit.sb13.monew.comment.service.CommentLikeService;
import com.codeit.sb13.monew.comment.service.dto.CommentLikeDto;
import com.codeit.sb13.monew.comment.service.dto.CommentLikeRegisterCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
@Tag(name = "댓글 관리", description = "댓글 관련 API")
public class CommentLikeController {

  private final CommentLikeService commentLikeService;

  @Operation(
      summary = "댓글 좋아요 등록",
      description = "사용자가 특정 댓글에 좋아요를 누르는 기능입니다. 같은 사용자는 같은 댓글에 중복 좋아요를 누를 수 없습니다."
  )
  @Parameters({
      @Parameter(
          name = "commentId",
          description = "좋아요를 누를 댓글의 ID",
          required = true,
          in = ParameterIn.PATH
      ),
      @Parameter(
          name = "Monew-Request-User-ID",
          description = "좋아요를 누른 요청자 ID",
          required = true,
          in = ParameterIn.HEADER
      )
  })
  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<CommentLikeDto> likeComment(@PathVariable UUID commentId, @RequestHeader("Monew-Request-User-ID") UUID requestUserId) {
    CommentLikeRegisterCommand command = new CommentLikeRegisterCommand(commentId, requestUserId);
    return ResponseEntity.status(HttpStatus.OK).body(commentLikeService.likeComment(command));
  }
}
