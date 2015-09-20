package org.multibit.hd.hardware.core.events;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
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
 * <li>Entry point to broadcast high level hardware wallet events to downstream consumers</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletEvents {

  private static final Logger log = LoggerFactory.getLogger(HardwareWalletEvents.class);

  /**
   * Dedicated thread for high level messages for asynchronous transmission
   */
  private static final ListeningExecutorService hardwareWalletEventService = SafeExecutors.newSingleThreadExecutor("hardware-wallet-events");

  /**
   * Use Guava to handle subscribers to events
   */
  private static final EventBus hardwareWalletEventBus = new EventBus(ExceptionHandler.newSubscriberExceptionHandler());

  /**
   * Keep track of the Guava event bus subscribers for a clean shutdown
   */
  private static final Set<Object> hardwareWalletEventBusSubscribers = Sets.newHashSet();


  /**
   * Utilities have a private constructor
   */
  private HardwareWalletEvents() {
  }

  /**
   * <p>Subscribe to events. Repeating a subscribe will not affect the event bus.</p>
   * <p>This approach ensures all subscribers will be correctly removed during a shutdown or wizard hide event</p>
   *
   * @param subscriber The subscriber (use the Guava <code>@Subscribe</code> annotation to subscribe a method)
   */
  public static void subscribe(Object subscriber) {

    Preconditions.checkNotNull(subscriber, "'subscriber' must be present");

    if (hardwareWalletEventBusSubscribers.add(subscriber)) {
      log.trace("Register: " + subscriber.getClass().getSimpleName());
      try {
        hardwareWalletEventBus.register(subscriber);
      } catch (IllegalArgumentException e) {
        log.warn("Unexpected failure to register");
      }
    } else {
      log.warn("Subscriber already registered: " + subscriber.getClass().getSimpleName());
    }

  }

  /**
   * <p>Unsubscribe a known subscriber from events. Providing an unknown object will not affect the event bus.</p>
   * <p>This approach ensures all subscribers will be correctly removed during a shutdown or wizard hide event</p>
   *
   * @param subscriber The subscriber (use the Guava <code>@Subscribe</code> annotation to subscribe a method)
   */
  public static void unsubscribe(Object subscriber) {

    Preconditions.checkNotNull(subscriber, "'subscriber' must be present");

    if (hardwareWalletEventBusSubscribers.contains(subscriber)) {
      log.trace("Unregister: " + subscriber.getClass().getSimpleName());
      try {
        hardwareWalletEventBus.unregister(subscriber);
      } catch (IllegalArgumentException e) {
        log.warn("Unexpected failure to unregister");
      }
      hardwareWalletEventBusSubscribers.remove(subscriber);
    } else {
      log.warn("Subscriber already unregistered: " + subscriber.getClass().getSimpleName());
    }

  }

  /**
   * <p>Unsubscribe all subscribers from events</p>
   * <p>This approach ensures all subscribers will be correctly removed during a shutdown or wizard hide event</p>
   */
  @SuppressWarnings("unchecked")
  public static void unsubscribeAll() {

    Set allSubscribers = Sets.newHashSet();
    allSubscribers.addAll(hardwareWalletEventBusSubscribers);
    for (Object subscriber : allSubscribers) {
      unsubscribe(subscriber);
    }
    allSubscribers.clear();
    log.info("All subscribers removed");

  }

  /**
   * <p>A hardware event can wrap a hardware wallet message adapted from a protocol buffer message</p>
   *
   * @param eventType The event type (e.g. SHOW_DEVICE_READY)
   * @param message   The message itself (from protocol buffers)
   * @param source    The client name acting as the source (e.g. "TREZOR", "KEEP_KEY" etc)
   */
  public static void fireHardwareWalletEvent(final HardwareWalletEventType eventType, final HardwareWalletMessage message, final String source) {

    Preconditions.checkNotNull(eventType, "'messageType' must be present");
    Preconditions.checkNotNull(message, "'message' must be present");

    final ListenableFuture<Boolean> future = hardwareWalletEventService.submit(
      new Callable<Boolean>() {
        @Override
        public Boolean call() {
          log.debug("Firing 'hardware wallet' event: {} for {}", eventType.name(), source);
          hardwareWalletEventBus.post(
            new HardwareWalletEvent(
              eventType,
              Optional.of(message),
              source));

          // Must be OK to be here
          return true;
        }
      });

    Futures.addCallback(
      future, new FutureCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          log.debug("Completed 'hardware wallet' event: {}", eventType.name());
        }

        @Override
        public void onFailure(Throwable t) {
          log.error("Failed to complete 'hardware wallet' event: {}", eventType.name(), t);
        }
      });

  }

  /**
   * <p>A hardware event can have no further information</p>
   *
   * @param eventType The event type (e.g. SHOW_DEVICE_READY)
   * @param source    The client name acting as the source (e.g. "TREZOR", "KEEP_KEY" etc)
   */
  public static void fireHardwareWalletEvent(final HardwareWalletEventType eventType, final String source) {

    Preconditions.checkNotNull(eventType, "'eventType' must be present");

    final ListenableFuture<Boolean> future = hardwareWalletEventService.submit(
      new Callable<Boolean>() {
        @Override
        public Boolean call() {
          log.debug("Firing 'hardware wallet' event: {} for {}", eventType.name(), source);
          hardwareWalletEventBus.post(
            new HardwareWalletEvent(
              eventType,
              Optional.<HardwareWalletMessage>absent(),
              source));

          // Must be OK to be here
          return true;
        }
      });

    Futures.addCallback(
      future, new FutureCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          log.debug("Completed 'hardware wallet' event: {}", eventType.name());
        }

        @Override
        public void onFailure(Throwable t) {
          log.error("Failed to complete 'hardware wallet' event: {}", eventType.name(), t);
        }
      });

  }

  /**
   * <p>A hardware event can wrap a hardware wallet message adapted from a protocol buffer message</p>
   *
   * @param event The event
   */
  public static void fireHardwareWalletEvent(final HardwareWalletEvent event) {

    Preconditions.checkNotNull(event, "'event' must be present");

    final ListenableFuture<Boolean> future = hardwareWalletEventService.submit(
      new Callable<Boolean>() {
        @Override
        public Boolean call() {
          log.debug("Firing 'hardware wallet' event: {} for {}", event, event.getSource());
          hardwareWalletEventBus.post(event);

          // Must be OK to be here
          return true;
        }
      });

    Futures.addCallback(
      future, new FutureCallback<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          log.debug("Completed 'hardware wallet' event: {}", event);
        }

        @Override
        public void onFailure(Throwable t) {
          log.error("Failed to complete 'hardware wallet' event: {}", event, t);
        }
      });
  }
}
