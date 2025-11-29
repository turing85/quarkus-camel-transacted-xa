package de.turing85.quarkus.camel.transacted.xa.route.helper;

import java.util.Objects;
import java.util.function.Consumer;

import jakarta.jms.JMSException;
import jakarta.jms.Message;

import lombok.RequiredArgsConstructor;
import org.apache.camel.component.jms.JmsMessage;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
public final class HasBodyMatcher extends BaseMatcher<Message> {
  private static final String BODY_MATCHER_PREFIX = "a message body that ";

  private final Matcher<Object> bodyMatcher;

  @Nullable
  private Consumer<Description> describer;

  @Nullable
  private Consumer<Description> mismatchDescriber;

  public static HasBodyMatcher hasBody(Object object) {
    return new HasBodyMatcher(CoreMatchers.is(object));
  }

  public static HasBodyMatcher hasBody(Matcher<Object> matcher) {
    return new HasBodyMatcher(matcher);
  }

  @Override
  public boolean matches(Object object) {
    if (!CoreMatchers.notNullValue().matches(object)) {
      describer = description -> {
        description.appendText("A message that is ");
        CoreMatchers.notNullValue().describeTo(description);
      };
      mismatchDescriber = description -> {
        description.appendText("was A message that ");
        CoreMatchers.notNullValue().describeMismatch(object, description);
      };
      return false;
    }
    if (object instanceof Message message) {
      return bodyMatches(message);
    }
    describer = description -> description
        .appendText("an instance of %s".formatted(JmsMessage.class.getName()));
    mismatchDescriber = description -> description.appendText("was %s".formatted(object));
    return false;
  }

  @Override
  public void describeTo(Description description) {
    Objects.requireNonNull(describer, "describer is null").accept(description);
  }

  @Override
  public void describeMismatch(Object item, Description description) {
    Objects.requireNonNull(mismatchDescriber, "mismatchDescriber is null").accept(description);
  }

  private boolean bodyMatches(Message message) {
    try {
      final Object messageBody = message.getBody(Object.class);
      if (!bodyMatcher.matches(messageBody)) {
        describer = description -> {
          description.appendText(BODY_MATCHER_PREFIX);
          bodyMatcher.describeTo(description);
        };
        mismatchDescriber = description -> {
          description.appendText("was " + BODY_MATCHER_PREFIX);
          bodyMatcher.describeMismatch(messageBody, description);
        };
        return false;
      }
      return true;
    } catch (JMSException e) {
      describer =
          description -> description.appendText("a message with a body transformable to String");
      mismatchDescriber = description -> description.appendText("was %s".formatted(message));
      return false;
    }
  }
}
