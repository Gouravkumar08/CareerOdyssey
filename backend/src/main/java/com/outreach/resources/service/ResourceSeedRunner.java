package com.outreach.resources.service;

import com.outreach.resources.dto.ResourceItemRequest;
import com.outreach.resources.repository.ResourceCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * One-time data seed for the Free Resources library, in lieu of a Flyway migration (none
 * exists in this project — schema is Hibernate {@code ddl-auto: update}). Runs once at
 * startup, guarded by {@code resourceCategoryRepository.count() == 0}, so it's safe to leave
 * in the codebase permanently: it only ever seeds an empty library, never touches
 * categories/items once any category exists — including categories created later by the
 * user through the app itself.
 * <p>
 * Transcribed verbatim from a user-provided curated link list, organized as
 * Category → optional Sub-category → link. Every seeded item's {@code important} flag is
 * left {@code false} — no importance was specified in the source, so none is fabricated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class ResourceSeedRunner implements ApplicationRunner {

    private final ResourceCategoryRepository categoryRepository;
    private final ResourceItemService itemService;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.info("Free Resources library already seeded — skipping.");
            return;
        }
        log.info("Seeding Free Resources library...");
        int total = 0;
        for (String line : DATA.strip().split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            ResourceItemRequest request = new ResourceItemRequest();
            request.setCategoryName(parts[0].trim());
            request.setSubCategory(blankToNull(parts[1]));
            request.setName(parts[2].trim());
            request.setLink(parts[3].trim());
            request.setImportant(false);
            itemService.create(request);
            total++;
        }
        log.info("Free Resources seed loaded: {} items.", total);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // category | subCategory | name | link   (subCategory may be blank)

    private static final String DATA = """
            Data Structures and Algorithm||YouTube video|https://www.youtube.com/watch?v=yRpLlJmRo2w&list=PLfqMhTWNBTe3LtFWcvwpqTkUSlB32kJop
            Data Structures and Algorithm||YouTube video|https://www.youtube.com/watch?v=rZ41y93P2Qo&list=PL9gnSGHSqcnr_DxHsP7AW9ftq0AtAyYqJ
            Data Structures and Algorithm||YouTube video|https://www.youtube.com/watch?v=RBSGKlAvoiM
            Data Structures and Algorithm||YouTube playlist|https://www.youtube.com/playlist?list=PL6Zs6LgrJj3tDXv8a_elC6eT_4R5gfX4d
            Data Structures and Algorithm||YouTube playlist|https://www.youtube.com/playlist?list=PLh_njhZ_MgInazcDWlX1RlpwIIYACSyIJ
            Coding Questions||takeuforward.org|https://takeuforward.org/interview-sheets/strivers-79-last-moment-dsa-sheet-ace-interviews/
            Coding Questions||YouTube video|https://www.youtube.com/watch?v=T0u5nwSA0w0
            Coding Questions||YouTube video|https://www.youtube.com/watch?v=PieZjz2Pyhw
            Coding Questions||YouTube video|https://www.youtube.com/watch?v=-OV83G_s4e8&list=PLoDervMHdCDIeZCycNfumh8PmhSIBcG0k
            Coding Questions||YouTube video|https://www.youtube.com/watch?v=kGN46Z3y4WM&list=PLoDervMHdCDJBy_6BEx4eUM2QvuYHFYja
            Coding Questions||YouTube playlist|https://www.youtube.com/playlist?list=PL1w8k37X_6L86f3PUUVFoGYXvZiZHde1S
            Coding Questions||YouTube playlist|https://www.youtube.com/playlist?list=PLh_njhZ_MgInv0amXTsZIIhTa3G3iNyZN
            Aptitude||indiabix.com|https://www.indiabix.com/
            Aptitude||YouTube video|https://www.youtube.com/watch?v=xMjf7pHPlCg&list=PLMufDeLh5x2CX3jjYFKRLO46xV_522wM0
            Aptitude||sanfoundry.com|https://www.sanfoundry.com/
            System Design||YouTube video|https://www.youtube.com/watch?v=Vnm-ycSfJx4
            System Design||YouTube video|https://www.youtube.com/watch?v=m8Icp_Cid5o&list=PLWKjhJtqVAbn21gs5UnLhCQ82f923WCgM
            System Design||YouTube playlist|https://www.youtube.com/playlist?list=PLMCXHnjXnTnvo6alSjVkgxV-VH6EPyvoX
            System Design||YouTube playlist|https://www.youtube.com/playlist?list=PLCRMIe5FDPsd0gVs500xeOewfySTsmEjf
            System Design||YouTube playlist|https://www.youtube.com/playlist?list=PLinedj3B30sBlBWRox2V2tg9QJ2zr4M3o
            System Design||YouTube channel — ConceptAndCodingByShrayansh|https://www.youtube.com/@ConceptAndCodingByShrayansh/playlists
            System Design||YouTube playlist|https://www.youtube.com/playlist?list=PLot-Xpze53le35rQuIbRET3YwEtrcJfdt
            System Design||YouTube playlist|https://www.youtube.com/playlist?list=PL564gOx0bCLqTolRIHIsR2JPv11w8LESW
            System Design||YouTube video|https://www.youtube.com/watch?v=F2FmTdLtb_4
            System Design||YouTube video|https://www.youtube.com/watch?v=43-X22tdxiI&list=PLA3GkZPtsafZdyC5iucNM_uhqGJ5yFNUM
            System Design||API Design|https://www.youtube.com/watch?v=Rrd6xkyjPB8
            System Design||YouTube playlist|https://www.youtube.com/playlist?list=PL_c9BZzLwBRLSs6x50D5WIH76VCUxJs9E
            System Design||Interview Questions|https://www.youtube.com/playlist?list=PLPtUyMfD0mNJDZg50fg2CptjLBavHot47
            Frontend Technologies|HTML|YouTube video|https://www.youtube.com/watch?v=iVCzmDwIQpA
            Frontend Technologies|CSS|YouTube video|https://www.youtube.com/watch?v=ft30zcMlFao
            Frontend Technologies|CSS|YouTube video|https://www.youtube.com/watch?v=WuiB5TAQOAM
            Frontend Technologies|JavaScript|YouTube video|https://www.youtube.com/watch?v=PkZNo7MFNFg&list=PLWKjhJtqVAbleDe3_ZA8h3AO2rXar-q2V
            Frontend Technologies|JavaScript|YouTube video|https://youtu.be/sscX432bMZo
            Frontend Technologies|JavaScript|YouTube video|https://www.youtube.com/watch?v=e2fKYP_7B_Y
            Frontend Technologies|JavaScript|YouTube video|https://www.youtube.com/watch?v=W6NZfCO5SIk
            Frontend Technologies|JavaScript|YouTube video|https://www.youtube.com/watch?v=1Sx-KgDI4Y0&list=PLA3GkZPtsafYWv89IsGYswgCGzVsd7mbY
            Frontend Technologies|React|YouTube video|https://www.youtube.com/watch?v=SqcY0GlETPk
            Frontend Technologies|React|YouTube video|https://www.youtube.com/watch?v=DLX62G4lc44&list=PLWKjhJtqVAbkArDMazoARtNz1aMwNWmvC
            Frontend Technologies|React|YouTube video|https://www.youtube.com/watch?v=G6D9cBaLViA
            Frontend Technologies|React|YouTube video|https://www.youtube.com/watch?v=LDB4uaJ87e0
            Frontend Technologies|Next js|YouTube video|https://www.youtube.com/watch?v=KjY94sAKLlw&list=PLWKjhJtqVAbmMuZ3saqRIBimAKIMYkt0E
            Frontend Technologies|Next js|YouTube video|https://www.youtube.com/watch?v=ZVnjOPwW4ZA
            Backend Technologies||JAVA from Basics to Advanced|https://www.youtube.com/playlist?list=PL6W8uoQQ2c63f469AyV78np0rbxRFppkx
            Backend Technologies||Spring Boot from Basics to Advanced|https://www.youtube.com/playlist?list=PL6W8uoQQ2c60g6_fcjDCLHSx1LBeVYqyZ
            Backend Technologies||Low Level Design from Basics to Advanced|https://www.youtube.com/playlist?list=PL6W8uoQQ2c61X_9e6Net0WdYZidm7zooW
            Backend Technologies||High Level Design from Basics to Advanced|https://www.youtube.com/playlist?list=PL6W8uoQQ2c63W58rpNFDwdrBnq5G3EfT7
            Backend Technologies||Distributed Microservices (Practical)|https://www.youtube.com/playlist?list=PL6W8uoQQ2c60yO3LbjYkpKikwgAZHAT5o
            Backend Technologies||JUnit5 and Mockito from Basics to Advanced|https://www.youtube.com/playlist?list=PL6W8uoQQ2c61_unqN5bY0kYp65Dt_Sm3c
            Backend Technologies||Event Driven Architecture|https://www.youtube.com/playlist?list=PL6W8uoQQ2c62YCR2kkE-0tg3-y1QwaIWk
            Backend Technologies||Spring AI|https://www.youtube.com/playlist?list=PL6W8uoQQ2c602ytEt7f_qVtlV7jNkdabL
            Backend Technologies|Node js|YouTube video|https://www.youtube.com/watch?v=RLtyhwFtXQA&list=PLWKjhJtqVAbmGQoa3vFjeRbRADAOC9drk
            Backend Technologies|Node js|YouTube video|https://www.youtube.com/watch?v=TlB_eWDSMt4
            Backend Technologies|Node js|YouTube playlist|https://www.youtube.com/playlist?list=PLu71SKxNbfoBGh_8p_NS-ZAh6v7HhYqHW
            Backend Technologies|Node js|YouTube video|https://www.youtube.com/watch?v=T55Kb8rrH1g&list=PLbtI3_MArDOkXRLxdMt1NOMtCS-84ibHH
            Backend Technologies|Java|YouTube video|https://www.youtube.com/watch?v=7-lK9EpBS_Y
            Backend Technologies|Java|YouTube video|https://www.youtube.com/watch?v=A74TOX803D0
            Backend Technologies|Java|YouTube channel — ConceptAndCodingByShrayansh|https://www.youtube.com/@ConceptAndCodingByShrayansh/playlists
            Backend Technologies|Java|YouTube video|https://www.youtube.com/watch?v=BGTx91t8q50
            Backend Technologies|Java|YouTube video|https://www.youtube.com/watch?v=xk4_1vDrzzo
            Backend Technologies|Java|YouTube video|https://www.youtube.com/watch?v=grEKMHGYyns
            Backend Technologies|Spring / Spring Boot|YouTube video|https://www.youtube.com/watch?v=-Fe0zk-F4OA
            Backend Technologies|Spring / Spring Boot|YouTube video|https://www.youtube.com/watch?v=1993zSY5UBI&list=PLA3GkZPtsafacdBLdd3p1DyRd5FGfr3Ue
            Backend Technologies|Spring / Spring Boot|YouTube video|https://www.youtube.com/watch?v=oeni_9g7too
            Backend Technologies|Spring / Spring Boot|YouTube channel — ConceptAndCodingByShrayansh|https://www.youtube.com/@ConceptAndCodingByShrayansh/playlists
            Backend Technologies|Redis|YouTube video|https://www.youtube.com/watch?v=XCsS_NVAa1g
            Backend Technologies|Redis|YouTube video|https://www.youtube.com/watch?v=Vx2zPMPvmug
            Database Management (Data Retrieval & Storage)|MySQL / Postgresql|YouTube video|https://www.youtube.com/watch?v=-fW2X7fh7Yg&list=PLWKjhJtqVAbn21gs5UnLhCQ82f923WCgM
            Database Management (Data Retrieval & Storage)|MySQL / Postgresql|YouTube video|https://www.youtube.com/watch?v=eMIxuk0nOkU
            Database Management (Data Retrieval & Storage)|MySQL / Postgresql|YouTube video|https://www.youtube.com/watch?v=HXV3zeQKqGY
            Database Management (Data Retrieval & Storage)|MySQL / Postgresql|YouTube video|https://www.youtube.com/watch?v=Tdl7CGnhPeA
            Database Management (Data Retrieval & Storage)|MySQL / Postgresql|YouTube video|https://www.youtube.com/watch?v=_XhfAVceFcI
            Database Management (Data Retrieval & Storage)|MySQL / Postgresql|YouTube video|https://www.youtube.com/watch?v=VWugHKeYhNk&list=PLA3GkZPtsafZj87jyyITew3oySyECQ4tS
            Database Management (Data Retrieval & Storage)|Mongodb|YouTube video|https://www.youtube.com/watch?v=4EjKroJCpFA&list=PLA3GkZPtsafZydhN4nP0h7hw7PQuLsBv1
            API Development & Integration (Connecting Frontend & Backend)||YouTube video|https://www.youtube.com/watch?v=VywxIQ2ZXw4
            Deployment (Hosting & Making the App Live)|AWS (EC2, S3, RDS)|YouTube video|https://www.youtube.com/watch?v=RrKRN9zRBWs&list=PLWKjhJtqVAbkzvvpY12KkfiIGso9A_Ixs
            Deployment (Hosting & Making the App Live)|AWS (EC2, S3, RDS)|YouTube video|https://www.youtube.com/watch?v=3hLmDS179YE
            Deployment (Hosting & Making the App Live)|Docker|YouTube video|https://www.youtube.com/watch?v=fqMOX6JJhGo&list=PLWKjhJtqVAbkzvvpY12KkfiIGso9A_Ixs
            Deployment (Hosting & Making the App Live)|Docker|YouTube video|https://www.youtube.com/watch?v=exmSJpJvIPs
            Deployment (Hosting & Making the App Live)|Docker|YouTube video|https://www.youtube.com/watch?v=31k6AtW-b3Y
            Deployment (Hosting & Making the App Live)|Docker|YouTube video|https://www.youtube.com/watch?v=kTp5xUtcalw
            Deployment (Hosting & Making the App Live)|Docker|YouTube video|https://www.youtube.com/watch?v=17Bl31rlnRM
            Deployment (Hosting & Making the App Live)|Docker|YouTube video|https://www.youtube.com/watch?v=KVBON1lA9N8&list=PL9gnSGHSqcnoqBXdMwUTRod4Gi3eac2Ak
            Performance Optimization & Scaling|Redis|YouTube video|https://www.youtube.com/watch?v=XCsS_NVAa1g
            Performance Optimization & Scaling|Redis|YouTube video|https://www.youtube.com/watch?v=Vx2zPMPvmug
            Performance Optimization & Scaling|Kafka|YouTube video|https://www.youtube.com/watch?v=ZJJHm_bd9Zo
            Performance Optimization & Scaling|Kafka|YouTube video|https://www.youtube.com/watch?v=2YnutJ8tNos
            Monitoring & Maintenance|CI/CD (GitHub Actions, Jenkins)|YouTube video|https://www.youtube.com/watch?v=apGV9Kg7ics&list=PL9gnSGHSqcnoqBXdMwUTRod4Gi3eac2Ak
            Monitoring & Maintenance|CI/CD (GitHub Actions, Jenkins)|YouTube video|https://www.youtube.com/watch?v=Ez8F0nW6S-w
            Monitoring & Maintenance|CI/CD (GitHub Actions, Jenkins)|YouTube video|https://www.youtube.com/watch?v=q8EevlEpQ2A
            Monitoring & Maintenance|CI/CD (GitHub Actions, Jenkins)|YouTube video|https://www.youtube.com/watch?v=6YZvp2GwT0A
            Monitoring & Maintenance|CI/CD (GitHub Actions, Jenkins)|YouTube video|https://www.youtube.com/watch?v=NnkUGzaqqOc
            CS - Fundamentals|OOPs|YouTube video|https://www.youtube.com/watch?v=BSVKUk58K6U&list=PL9gnSGHSqcno1G3XjUbwzXHL8_EttOuKk
            CS - Fundamentals|OOPs|YouTube video|https://www.youtube.com/watch?v=bSrm9RXwBaI
            CS - Fundamentals|OOPs|geeksforgeeks.org|https://www.geeksforgeeks.org/oops-interview-questions/
            CS - Fundamentals|OS|YouTube video|https://www.youtube.com/watch?v=8XBtAjKwCm4
            CS - Fundamentals|OS|naukri.com|https://www.naukri.com/code360/library/operating-system-interview-questions
            CS - Fundamentals|DBMS|YouTube video|https://www.youtube.com/watch?v=pPqazMTzNOM
            CS - Fundamentals|DBMS|YouTube video|https://www.youtube.com/watch?v=c5HAwKX-suM
            CS - Fundamentals|DBMS|simplilearn.com|https://www.simplilearn.com/dbms-interview-questions-and-answers-article
            CS - Fundamentals|CN|YouTube video|https://www.youtube.com/watch?v=IPvYjXCsTg8
            Projects||YouTube video|https://www.youtube.com/watch?v=5ZdHfJVAY-s
            Projects||YouTube video|https://www.youtube.com/watch?v=zLJoVRleOuc
            Projects||YouTube video|https://www.youtube.com/watch?v=VsUzmlZfYNg
            Projects||YouTube playlist|https://www.youtube.com/playlist?list=PLChiukrA-RMOEB1PRQqB1NITIRsDz9pIN
            Projects||nice project-nextjs|https://www.youtube.com/watch?v=XxXyfkrP298&list=PL6QREj8te1P6wX9m5KnicnDVEucbOPsqR&index=7
            Projects||YouTube playlist|https://www.youtube.com/playlist?list=PL6QREj8te1P6wX9m5KnicnDVEucbOPsqR
            """;
}
