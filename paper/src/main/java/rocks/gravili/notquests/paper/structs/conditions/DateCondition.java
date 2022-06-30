/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class DateCondition extends Condition {

  private int year = -1;
  private int month = -1;
  private int day = -1;
  private int hours = -1;
  private int minutes = -1;
  private int seconds = -1;
  private String operation = "";

  private TimeZone timeZone = null;

  public DateCondition(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ConditionFor conditionFor) {
    final CommandFlag<Integer> year =
        CommandFlag.newBuilder("year")
            .withArgument(IntegerArgument.<CommandSender>newBuilder("year").withMin(0))
            .withDescription(ArgumentDescription.of("Enter year."))
            .build();
    final CommandFlag<Integer> month =
        CommandFlag.newBuilder("month")
            .withArgument(IntegerArgument.<CommandSender>newBuilder("month").withMin(0).withMax(12))
            .withDescription(ArgumentDescription.of("Enter month."))
            .build();
    final CommandFlag<Integer> day =
        CommandFlag.newBuilder("day")
            .withArgument(IntegerArgument.<CommandSender>newBuilder("day").withMin(0).withMax(31))
            .withDescription(ArgumentDescription.of("Enter day."))
            .build();
    final CommandFlag<Integer> hours =
        CommandFlag.newBuilder("hours")
            .withArgument(IntegerArgument.<CommandSender>newBuilder("hours").withMin(0).withMax(24))
            .withDescription(ArgumentDescription.of("Enter hours."))
            .build();

    final CommandFlag<Integer> minutes =
        CommandFlag.newBuilder("minutes")
            .withArgument(
                IntegerArgument.<CommandSender>newBuilder("minutes").withMin(0).withMax(60))
            .withDescription(ArgumentDescription.of("Enter minutes."))
            .build();

    final CommandFlag<Integer> seconds =
        CommandFlag.newBuilder("seconds")
            .withArgument(
                IntegerArgument.<CommandSender>newBuilder("seconds").withMin(0).withMax(60))
            .withDescription(ArgumentDescription.of("Enter seconds."))
            .build();

    final CommandFlag<String> timeZone =
        CommandFlag.newBuilder("timeZone")
            .withArgument(
                StringArgument.<CommandSender>newBuilder("timeZone")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[timezone]",
                                  "");

                          return new ArrayList<String>(Arrays.asList(TimeZone.getAvailableIDs()));
                        })
                    .single()
                    .build())
            .withDescription(ArgumentDescription.of("Enter time zone."))
            .build();

    manager.command(
        builder
            .argument(
                StringArgument.<CommandSender>newBuilder("Date operation")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Date operation]",
                                  "<optional flags>");

                          ArrayList<String> completions = new ArrayList<>();
                          completions.add("after");
                          completions.add("before");
                          return completions;
                        })
                    .single()
                    .build(),
                ArgumentDescription.of("Date operation"))
            .flag(year)
            .flag(month)
            .flag(day)
            .flag(hours)
            .flag(minutes)
            .flag(seconds)
            .flag(timeZone)
            .handler(
                (context) -> {
                  final String operation = context.get("Date operation");

                  if (!operation.equalsIgnoreCase("after")
                      && !operation.equalsIgnoreCase("before")) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The date operation can only be <highlight>after</highlight> or <highlight>before</highlight>."));
                    return;
                  }

                  final int yearValue = context.flags().getValue(year, -1);
                  final int monthValue = context.flags().getValue(month, -1);
                  final int dayValue = context.flags().getValue(day, -1);
                  final int hoursValue = context.flags().getValue(hours, -1);
                  final int minutesValue = context.flags().getValue(minutes, -1);
                  final int secondsValue = context.flags().getValue(seconds, -1);

                  final String timeZoneValue = context.flags().getValue(timeZone, "");

                  final TimeZone timeZoneObjectValue = TimeZone.getTimeZone(timeZoneValue);

                  DateCondition dateCondition = new DateCondition(main);
                  dateCondition.setYear(yearValue);
                  dateCondition.setMonth(monthValue);
                  dateCondition.setDay(dayValue);
                  dateCondition.setHours(hoursValue);
                  dateCondition.setMinutes(minutesValue);
                  dateCondition.setSeconds(secondsValue);
                  dateCondition.setTimeZone(timeZoneObjectValue);

                  dateCondition.setOperation(operation.toLowerCase(Locale.ROOT));

                  main.getConditionsManager().addCondition(dateCondition, context, conditionFor);
                }));
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  @Override
  public String checkInternally(final QuestPlayer questPlayer) {
    final LocalDateTime currentTime =
        timeZone == null ? LocalDateTime.now() : LocalDateTime.now(timeZone.toZoneId());

    final LocalDateTime timeToCompare =
        LocalDateTime.of(
            getYear() > -1 ? getYear() : currentTime.getYear(),
            getMonth() > -1 ? getMonth() : currentTime.getMonthValue(),
            getDay() > -1 ? getDay() : currentTime.getDayOfMonth(),
            getHours() > -1 ? getHours() : currentTime.getHour(),
            getMinutes() > -1 ? getMinutes() : currentTime.getMinute(),
            getSeconds() > -1 ? getSeconds() : currentTime.getSecond());

    if (operation.equals("before")) {
      if (!currentTime.isBefore(timeToCompare)) {
        return "<YELLOW>The current date needs to be before the " + timeToCompare.toString();
      }
    } else if (operation.equals("after")) {
      if (!currentTime.isAfter(timeToCompare)) {
        return "<YELLOW>The current date needs to be after the " + timeToCompare.toString();
      }
    } else {
      return "<error>Invalid date operator: <highlight>" + operation + "</highlight>.";
    }

    return "";
  }

  @Override
  public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
    final LocalDateTime currentTime =
        timeZone == null ? LocalDateTime.now() : LocalDateTime.now(timeZone.toZoneId());

    final LocalDateTime timeToCompare =
        LocalDateTime.of(
            getYear() > -1 ? getYear() : currentTime.getYear(),
            getMonth() > -1 ? getMonth() : currentTime.getMonthValue(),
            getDay() > -1 ? getDay() : currentTime.getDayOfMonth(),
            getHours() > -1 ? getHours() : currentTime.getHour(),
            getMinutes() > -1 ? getMinutes() : currentTime.getMinute(),
            getSeconds() > -1 ? getSeconds() : currentTime.getSecond());
    if (operation.equals("before")) {
      return "<GRAY>- Current date before " + timeToCompare.toString();

    } else if (operation.equals("after")) {
      return "<GRAY>- Current date after " + timeToCompare.toString();
    }
    return "<error>Invalid date operator: <highlight>" + operation + "</highlight>.";
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.year", getYear());
    configuration.set(initialPath + ".specifics.month", getMonth());
    configuration.set(initialPath + ".specifics.day", getDay());
    configuration.set(initialPath + ".specifics.hours", getHours());
    configuration.set(initialPath + ".specifics.minutes", getMinutes());
    configuration.set(initialPath + ".specifics.seconds", getSeconds());
    configuration.set(initialPath + ".specifics.operation", getOperation());
    configuration.set(
        initialPath + ".specifics.timeZone", getTimeZone() != null ? getTimeZone().getID() : null);
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    setYear(configuration.getInt(initialPath + ".specifics.year", -1));
    setMonth(configuration.getInt(initialPath + ".specifics.month", -1));
    setDay(configuration.getInt(initialPath + ".specifics.day"));
    setHours(configuration.getInt(initialPath + ".specifics.hours"));
    setMinutes(configuration.getInt(initialPath + ".specifics.minutes"));
    setSeconds(configuration.getInt(initialPath + ".specifics.seconds"));
    setOperation(configuration.getString(initialPath + ".specifics.operation", ""));
    final String timeZoneString = configuration.getString(initialPath + ".specifics.operation", "");
    if (!timeZoneString.isBlank()) {
      setTimeZone(TimeZone.getTimeZone(timeZoneString));
    }
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    operation = arguments.get(0);
    year = Integer.parseInt(arguments.get(1));
    month = Integer.parseInt(arguments.get(2));
    day = Integer.parseInt(arguments.get(3));
    hours = Integer.parseInt(arguments.get(4));
    minutes = Integer.parseInt(arguments.get(5));
    seconds = Integer.parseInt(arguments.get(6));

    timeZone = arguments.get(7).isBlank() ? null : TimeZone.getTimeZone(arguments.get(7));
    // maxTime = Integer.parseInt(arguments.get(1));
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

  public int getHours() {
    return hours;
  }

  public void setHours(int hours) {
    this.hours = hours;
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    this.minutes = minutes;
  }

  public int getSeconds() {
    return seconds;
  }

  public void setSeconds(int seconds) {
    this.seconds = seconds;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }
}
