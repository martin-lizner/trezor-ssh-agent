package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.protobuf.Message;
import org.multibit.commons.concurrent.SafeExecutors;
import org.multibit.hd.hardware.core.ExceptionHandler;
import org.multibit.hd.hardware.core.messages.HardwareWalletMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * <p>Factory to provide the following to application API:</p>
 * <ul>
 * <li>Entry point to broadcast the low level message events to the service</li>
 * </ul>
 * <p>Typically downstream consumers would not subscribe to these events. However
 * it can be useful for mocking clients and so on. If this is of interest to you
 * please refer to the MultiBit HD code (MIT licence) for extensive examples of
 * this that could save you a lot of time.</p>
 *
 * @since 0.0.1
 *  
 */
public class MessageEvents {

  private static final Logger log = LoggerFactory.getLogger(MessageEvents.class);

  /**
   * Dedicated thread for low level messages for asynchronous transmission
   */
  private static final ListeningExecutorService messageEventService = SafeExecutors.newSingleThreadExecutor("message-events");

  /**
   * Use Guava to handle subscribers to events
   */
  private static final EventBus messageEventBus = new EventBus(ExceptionHandler.newSubscriberExceptionHandler());

  /**
   * Keep track of the Guava event bus subscribers for a clean shutdown
   */
  private static final Set<Object> messageEventBusSubscribers = Sets.newHashSet();

  /**
   * Utilities have a private constructor
   */
  private MessageEvents() {
  }

  /**
   * <p>Subscribe to events. Repeating a subscribe will not affect the event bus.</p>
   * <p>This approach ensures all subscribers will be correctly removed during a shutdown</p>
   *
   * @param subscriber The subscriber (use the Guava <code>@Subscribe</code> annotation to subscribe a method)
   */
  public static void subscribe(Object subscriber) {

    Preconditions.checkNotNull(subscriber, "'subscriber' must be present");

    if (messageEventBusSubscribers.add(subscriber)) {
      log.trace("Register: " + subscriber.getClass().getSimpleName());
      try {
        messageEventBus.register(subscriber);
      } catch (IllegalArgumentException e) {
        log.warn("Unexpected failure to register");
      }
    } else {
      log.warn("Subscriber already registered: " + subscriber.getClass().getSimpleName());
    }

  }

  /**
   * <p>Unsubscribe a known subscriber from events. Providing an unknown object will not affect the event bus.</p>
   * <p>This approach ensures all subscribers will be correctly removed during a shutdown</p>
   *
   * @param subscriber The subscriber (use the Guava <code>@Subscribe</code> annotation to subscribe a method)
   */
  public static void unsubscribe(Object subscriber) {

    Preconditions.checkNotNull(subscriber, "'subscriber' must be present");

    if (messageEventBusSubscribers.contains(subscriber)) {
      log.trace("Unregister: " + subscriber.getClass().getSimpleName());
      try {
        messageEventBus.unregister(subscriber);
      } catch (IllegalArgumentException e) {
        log.warn("Unexpected failure to unregister");
      }
      messageEventBusSubscribers.remove(subscriber);
    } else {
      log.warn("Subscriber already unregistered: " + subscriber.getClass().getSimpleName());
    }

  }

  /**
   * <p>Unsubscribe all subscribers from events</p>
   * <p>This approach ensures all subscribers will be correctly removed during a shutdown</p>
   */
  @SuppressWarnings("unchecked")
  public static void unsubscribeAll() {

    Set allSubscribers = Sets.newHashSet();
    allSubscribers.addAll(messageEventBusSubscribers);
    for (Object subscriber : allSubscribers) {
      unsubscribe(subscriber);
    }
    allSubscribers.clear();
    log.info("All subscribers removed");

  }

  /**
   * <p>Convenience method to fire a message event</p>
   *
   * @param event The event (e.g. DEVICE_CONNECTED)
   */
  public static void fireMessageEvent(final MessageEvent event) {

    Preconditions.checkNotNull(event, "'messageType' must be present");

    final ListenableFuture<Boolean> future = messageEventService.submit(
      new Callable<Boolean>() {
        @Override
        public Boolean call() {
          log.debug("Firing 'message' event: {} for {}", event.getEventType().name(), event.getSource());
          messageEventBus.post(event);

          // Must be OK to be here
          return true;
        }
      });

    Futures.addCallback(
      future, new FutureCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          log.debug("Completed 'message' event: {}", event.getEventType().name());
        }

        @Override
        public void onFailure(Throwable t) {
          log.error("Failed to complete 'message' event: {}", event.getEventType().name(), t);
        }
      });

  }

  /**
   * <p>A message event without a protobuf message is used for communicating system status changes (e.g. DISCONNECT)</p>
   *
   * @param messageEventType The message type (e.g. DEVICE_CONNECTED)
   * @param source           The client name acting as the source (e.g. "TREZOR", "KEEP_KEY" etc)
   */
  public static void fireMessageEvent(final MessageEventType messageEventType, final String source) {

    Preconditions.checkNotNull(messageEventType, "'messageType' must be present");

    final ListenableFuture<Boolean> future = messageEventService.submit(
      new Callable<Boolean>() {
        @Override
        public Boolean call() {
          log.debug("Firing 'message' event type: {} for {}", messageEventType.name(), source);
          messageEventBus.post(
            new MessageEvent(
              messageEventType,
              Optional.<HardwareWalletMessage>absent(),
              Optional.<Message>absent(),
              source));

          // Must be OK to be here
          return true;
        }
      });

    Futures.addCallback(
      future, new FutureCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          log.debug("Completed 'message' event: {}", messageEventType.name());
        }

        @Override
        public void onFailure(Throwable t) {
          log.error("Failed to complete 'message' event: {}", messageEventType.name(), t);
        }
      });

  }

}
