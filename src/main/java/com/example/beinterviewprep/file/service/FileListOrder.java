package com.example.beinterviewprep.file.service;

import com.example.beinterviewprep.common.error.BadRequestException;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class FileListOrder {

  private static final Map<String, String> SORTABLE_PROPERTIES =
      Map.of("uploadedAt", "uploadedAt", "originalName", "originalName", "size", "sizeBytes");
  private static final Sort TIE_BREAKER = Sort.by("id");

  private FileListOrder() {}

  static Pageable toEntityPageable(Pageable requested) {
    Sort sort = Sort.by(requested.getSort().map(FileListOrder::toEntityOrder).toList());
    return PageRequest.of(
        requested.getPageNumber(), requested.getPageSize(), sort.and(TIE_BREAKER));
  }

  private static Sort.Order toEntityOrder(Sort.Order order) {
    String property = SORTABLE_PROPERTIES.get(order.getProperty());
    if (property == null) {
      throw new BadRequestException(
          "Cannot sort by '"
              + order.getProperty()
              + "'; allowed properties are uploadedAt, originalName and size");
    }
    return order.withProperty(property);
  }
}
