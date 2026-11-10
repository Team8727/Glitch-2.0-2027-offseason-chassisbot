package frc.robot.Autos;

import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.path.PathPlannerPath;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PathPlannerFilesTest {
    @Test
    void loadsDriveForwardPath() throws IOException, ParseException {
        assertNotNull(PathPlannerPath.fromPathFile("DriveForward"));
    }

    @Test
    void loadsCirclePath() throws IOException, ParseException {
        assertNotNull(PathPlannerPath.fromPathFile("Circle"));
    }

    @Test
    void loadsRobotConfig() throws IOException, ParseException {
        assertNotNull(RobotConfig.fromGUISettings());
    }
}
