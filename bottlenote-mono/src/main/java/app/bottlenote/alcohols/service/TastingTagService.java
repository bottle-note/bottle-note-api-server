package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TastingTagService {

  private final TastingTagRepository tastingTagRepository;

  private volatile Trie trie;

  @EventListener(ApplicationReadyEvent.class)
  @Scheduled(cron = "0 0 0 * * *")
  public void initializeTrie() {
    List<TastingTag> tags = tastingTagRepository.findAll();

    Trie.TrieBuilder builder = Trie.builder().ignoreCase();
    for (TastingTag tag : tags) {
      builder.addKeyword(tag.getKorName());
      builder.addKeyword(tag.getEngName());
    }

    this.trie = builder.build();
    log.info("TastingTag Trie 초기화 완료: {}개 태그 등록", tags.size());
  }

  /**
   * 문장에서 태그 이름 목록을 추출한다.
   *
   * @param text 분석할 문장
   * @return 매칭된 태그 이름 목록
   */
  public List<String> extractTagNames(String text) {
    if (trie == null || text == null || text.isBlank()) {
      return List.of();
    }

    return trie.parseText(text).stream()
        .filter(emit -> isWholeWord(text, emit))
        .map(Emit::getKeyword)
        .distinct()
        .toList();
  }

  private boolean isWholeWord(String text, Emit emit) {
    int start = emit.getStart();
    int end = emit.getEnd() + 1;

    if (start > 0 && isKorean(text.charAt(start - 1))) {
      return false;
    }
    if (end < text.length() && isKorean(text.charAt(end))) {
      return false;
    }

    return true;
  }

  private boolean isKorean(char c) {
    return (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x1100 && c <= 0x11FF);
  }
}
