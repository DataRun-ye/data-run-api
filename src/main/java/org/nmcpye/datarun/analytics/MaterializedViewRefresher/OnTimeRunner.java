package org.nmcpye.datarun.analytics.MaterializedViewRefresher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.annotation.Transactional;

/// @author Hamza Assada
/// @since 24/04/2025
//@Component
@Transactional
@Slf4j
public class OnTimeRunner implements CommandLineRunner {
    final MaterializedViewRefresher refresher;

    public OnTimeRunner(MaterializedViewRefresher refresher) {
        this.refresher = refresher;
    }

    @Override
    public void run(String... args) throws Exception {
        refresher.scheduledRefreshCheck();
    }
}
