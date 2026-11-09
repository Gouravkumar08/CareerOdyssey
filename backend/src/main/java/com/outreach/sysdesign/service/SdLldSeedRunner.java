package com.outreach.sysdesign.service;

import com.outreach.sysdesign.dto.SdItemRequest;
import com.outreach.sysdesign.dto.SdSheetRequest;
import com.outreach.sysdesign.dto.SdSheetResponse;
import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.repository.SdSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Second one-time data seed, sibling to {@link SdSeedRunner}. That runner's guard
 * ({@code sdSheetRepository.count() > 0}) already blocks once the "HLD Roadmap" sheet exists, so
 * this runner has its own, independent guard keyed on whether an "LLD Roadmap" sheet already
 * exists — it never touches {@link SdSeedRunner} or the HLD data.
 * <p>
 * Loads a single reference sheet, "LLD Roadmap", transcribed verbatim from a user-provided list of
 * low-level design pattern write-ups and practical LLD case studies. Every seeded item uses
 * {@link Category#LLD} and {@code important = false} — no importance is guessed. Two source rows
 * (HashMap Internal Implementation, Inventory Management System) carry GitLab links that look
 * truncated mid-path in the source; they are transcribed exactly as given rather than "fixed" or
 * guessed at. Where a source row had two links, the primary one became {@code resourceLink} and
 * the secondary one is mentioned as prose in {@code description}. Like {@link SdSeedRunner}, this
 * runner only ever creates catalog rows (topic/items/sheet) and never touches
 * {@code UserSdItemProgress}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SdLldSeedRunner implements ApplicationRunner {

    private static final String SHEET_NAME = "LLD Roadmap";
    private static final Long SEED_USER_ID = 1L;

    private final SdSheetRepository sdSheetRepository;
    private final SdSheetService sdSheetService;

    @Override
    public void run(ApplicationArguments args) {
        if (sdSheetRepository.findByNameIgnoreCase(SHEET_NAME).isPresent()) {
            log.info("'{}' sheet already present — skipping LLD seed.", SHEET_NAME);
            return;
        }
        log.info("Seeding System Design LLD reference sheet...");

        SdSheetRequest sheetRequest = new SdSheetRequest();
        sheetRequest.setName(SHEET_NAME);
        sheetRequest.setDescription(
                "Low-level design roadmap: GoF design patterns grouped by category, plus practical LLD "
                        + "case studies (Parking Lot, BookMyShow, Splitwise, etc.) with reference "
                        + "implementations.");
        SdSheetResponse sheet = sdSheetService.create(sheetRequest);

        int count = 0;
        for (String line : ITEMS.strip().split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            SdItemRequest request = new SdItemRequest();
            request.setTopicName(parts[0].trim());
            request.setSubTopic(blankToNull(parts[1]));
            request.setTitle(parts[2].trim());
            request.setCategory(Category.LLD);
            request.setImportant(false);
            request.setResourceLink(blankToNull(parts[3]));
            request.setDescription(blankToNull(parts[4]));
            sdSheetService.addItem(sheet.getId(), request, SEED_USER_ID);
            count++;
        }
        log.info("Seeded sheet '{}' with {} item rows.", SHEET_NAME, count);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // topic | subTopic | title | resourceLink | description   (subTopic/resourceLink/description may be blank)

    private static final String ITEMS = """
            Design Patterns|Creational|SOLID|https://drive.google.com/drive/folders/14Bigi6TB53HHjFJX8h5SjwZky4hJOO-W?usp=drive_link|
            Design Patterns|Creational|Abstract Factory Pattern|https://drive.google.com/file/d/1iyouXyHYdbhTyptjN17oqFW8RmGKTQ87/view?usp=drive_link|
            Design Patterns|Creational|Factory Method Pattern|https://drive.google.com/file/d/1nsb2UJ1y8YTYSCllNDu7xx-YuxfRRUAg/view?usp=drive_link|
            Design Patterns|Creational|Builder Design Pattern|https://drive.google.com/file/d/1V5qnfvYpNiEzb3bu9221HMD8QDaTkVC9/view?usp=drive_link|
            Design Patterns|Creational|Singleton|https://drive.google.com/file/d/1P_ptiVovd0ERLtMqVbgJVd9r5mYme-k_/view?usp=drive_link|
            Design Patterns|Creational|Object Pool Design Pattern|https://drive.google.com/file/d/1NymzCEjJq7c_w1XQrjpt77xk-M1KrvWo/view?usp=drive_link|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/creationalpatterns/objectpool?ref_type=heads
            Design Patterns|Creational|Creational Patterns — Overview|https://drive.google.com/drive/folders/19th5gDxaDXI15eXolORUhWh_Uoclm8LP?usp=drive_link|
            Design Patterns|Structural|Decorator Pattern|https://drive.google.com/file/d/19W73JBOtOtOzIHmKBA-4ACqCXwv60pB0/view?usp=drive_link|
            Design Patterns|Structural|Proxy Design Pattern|https://drive.google.com/file/d/1S0fRkyGAmhGupC02Xqi6YEfMmeaBxi_K/view?usp=drive_link|
            Design Patterns|Structural|Design File System Using Composite Design Pattern|https://drive.google.com/file/d/1FIBgNSJY-YkZYJO7WIKsY_io6tVIBkCa/view?usp=drive_link|
            Design Patterns|Structural|Adapter Design Pattern With Examples|https://drive.google.com/file/d/1BKx6HITzyw-c1RjX4372aH_L4b1-5S1w/view?usp=drive_link|
            Design Patterns|Structural|Facade Design Pattern|https://drive.google.com/file/d/1YpWHwZmOFIlFd-ftign3jtZWhOWEjwy0/view?usp=drive_link|
            Design Patterns|Structural|Bridge Design Pattern|https://drive.google.com/file/d/1byU7bYbjdkhIFU2je3Ck8IqjrFZvrRkr/view?usp=drive_link|
            Design Patterns|Structural|Design Word Processor Using Flyweight Design Pattern|https://drive.google.com/file/d/1oCmas08vpcpiEMqYkIYEvjkNWIQ9lDGm/view?usp=drive_link|
            Design Patterns|Structural|Structural Patterns — Overview|https://notebook.zohopublic.in/public/notes/74tdo33c5b7060d544b1c986e519dd3ba4d1b|Also see: https://drive.google.com/drive/folders/13hg1N0piFmP4uBEH3jd9fvjIoA4SK4Dt?usp=drive_link
            Design Patterns|Behavioral|Strategy Pattern|https://drive.google.com/file/d/1smTeCvuc9aQwZS7gd-vxPs75wezuYAFp/view?usp=drive_link|
            Design Patterns|Behavioral|Observer Pattern|https://drive.google.com/file/d/1l3T-TsuhWyQx8us5lxeh-bt4KR_8tr9w/view?usp=drive_link|
            Design Patterns|Behavioral|Chain of Responsibility|https://drive.google.com/file/d/1niWAA5Vy3gC8i6xU96igcYVR_ygZmboT/view?usp=drive_link|
            Design Patterns|Behavioral|Null Object Pattern|https://drive.google.com/file/d/1LUQFI4vDRn6mAsgmDMVCO4lmOXX50dDc/view?usp=drive_link|
            Design Patterns|Behavioral|Design Undo, Redo Feature with Command Pattern|https://drive.google.com/file/d/183IjgJ6pQDHswDQiwWMazetTpdK6SWaV/view?usp=drive_link|
            Design Patterns|Behavioral|Iterator Design Pattern|https://drive.google.com/file/d/1-AR6mJkZydatqHIxA58EreRzDt33_MqQ/view?usp=drive_link|
            Design Patterns|Behavioral|Mediator Pattern|https://drive.google.com/file/d/1SVx1ZYrJ_nqdHQ6Uq390wSt1TMX5vPOU/view?usp=drive_link|
            Design Patterns|Behavioral|Visitor Design Pattern|https://drive.google.com/file/d/1vX_USKxmI-wqOwsV3tMD4OGj6a-usJdn/view?usp=drive_link|
            Design Patterns|Behavioral|Memento Design Pattern|https://drive.google.com/file/d/1hc0cbjPKURcsvlMMkfMsq4yFXisS9Z2b/view?usp=drive_link|
            Design Patterns|Behavioral|Template Method Design Pattern|https://drive.google.com/file/d/15qaWbbBxXeZD3uC_7jfVHwPZxxiAm-jV/view?usp=drive_link|
            Design Patterns|Behavioral|Interpreter Design Pattern|https://drive.google.com/file/d/1jnzOCqy1ICoPkI0d-N9HolZacVtlbeJD/view?usp=drive_link|
            Design Patterns|Architectural|MVC|https://drive.google.com/file/d/1oKEaL_-2SnfeRIJUDzX7WNEOtqqcMTJ0/view?usp=drive_link|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/additionalpatterns/mvc?ref_type=heads
            LLD Case Studies|Practical Design|Parking Lot|https://drive.google.com/file/d/1aE9QhlRZgtUDI-p_2NiPdVWaq3eP-Mn7/view?usp=drive_link|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/parking_lot?ref_type=heads
            LLD Case Studies|Practical Design|Design Tic Tac Toe Game|https://drive.google.com/file/d/1JgTipI2hutFrd3YxDpE8L2KukIr0VC7H/view?usp=drive_link|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/tictactoe
            LLD Case Studies|Practical Design|Elevator System|https://drive.google.com/file/d/1MGudMSiJj2JMC75qq3o34gW2Kz-UAYPl/view?usp=drive_link|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/elevator
            LLD Case Studies|Practical Design|Car Rental System|https://drive.google.com/file/d/1QR51mRs_1cM7Cq5aDlPLKAvrjHMtLiU0/view?usp=drive_link|Part 2: https://drive.google.com/file/d/11UNsQAciYNTr1iDkkR_H9hJ5lj4A4kr4/view?usp=drive_link
            LLD Case Studies|Practical Design|Snake and Ladder Game|https://drive.google.com/file/d/1cGyGGWOrp_b4Jm2xHFlI_FitPivDHgiN/view?usp=drive_link|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/snakeNladder?ref_type=heads
            LLD Case Studies|Practical Design|HashMap Internal Implementation in Java|https://gitlab.com/shrayansh8/interviewcodingpractise/-/blob/main/src/main/java/com/conceptandcoding/LowLevelDesign|
            LLD Case Studies|Practical Design|BookMyShow|https://drive.google.com/file/d/1XbWo8m1hOOdKGww0UO1x3zIskdQjqf2i/view?usp=drive_link|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/book_my_show?ref_type=heads
            LLD Case Studies|Practical Design|Design Vending Machine|https://drive.google.com/file/d/1FDEdh2xhvlZpftC_392pml9h2lcl_q3A/view?usp=drive_link|
            LLD Case Studies|Practical Design|ATM|https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/atm?ref_type=heads|
            LLD Case Studies|Practical Design|Chess Mock Interview|https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/chess?ref_type=heads|
            LLD Case Studies|Practical Design|Splitwise|https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/splitwise?ref_type=heads|(no separate written notes available)
            LLD Case Studies|Practical Design|Cricbuzz|https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/cricbuzz?ref_type=heads|
            LLD Case Studies|Practical Design|Inventory Management System|https://gitlab.com/shrayansh8/interviewcodingpractise/-/tree/main/src/main/java/com/conceptandcoding/LowLevelDesign|
            LLD Case Studies|Practical Design|Apply Coupon on Shopping Cart|https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/couponappln?ref_type=heads|
            LLD Case Studies|Practical Design|Payment Gateway|https://notebook.zohopublic.in/public/notes/bietv6993f5a023ec4e9f89436149ac3ed2f7|Reference implementation: https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/paymentgateway?ref_type=heads
            LLD Case Studies|Practical Design|Meeting Scheduler Mock Interview|https://gitlab.com/shrayansh8/lld-lowleveldesign/-/tree/main/src/main/java/com/conceptcoding/interviewquestions/meetingscheduler?ref_type=heads|
            """;
}
