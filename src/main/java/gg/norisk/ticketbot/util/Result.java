package gg.norisk.ticketbot.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Result<T> {
  @Nullable @Getter private final T value;
  @Nullable @Getter private final String error;

  private Result(@Nullable T value, @Nullable String error) {
    this.value = value;
    this.error = error;
  }

  public static <T> Result<T> failure(@NotNull String error) {
    return new Result<>(null, error);
  }

  public static <T> Result<T> success(T value) {
    return new Result<>(value, null);
  }

  public boolean isFailure() {
    return error != null;
  }
}
