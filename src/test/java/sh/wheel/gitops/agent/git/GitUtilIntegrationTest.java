package sh.wheel.gitops.agent.git;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class GitUtilIntegrationTest {

    @Test
    void checkout() throws GitAPIException, IOException {
        Path target = Paths.get("/tmp/repo");

        GitUtil.checkoutLatest("https://github.com/wheel-sh/wheel-agent", "master", target);

        Assertions.assertTrue(Files.exists(target));
    }
}