package frc.robot.Commands;

import com.pathplanner.lib.commands.PathPlannerAuto;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Subsystem;
import frc.robot.Subsystems.DriveSubsystem;

public class AutonomousRoutine extends Command {
  private final Command autoCommand;

  public AutonomousRoutine() {
    autoCommand = new PathPlannerAuto("pathplanner/auto.routine", true);
    addRequirements(autoCommand.getRequirements().toArray(new Subsystem[0]));
  }

  @Override
  public void initialize() {
    DriveSubsystem.getInstance().resetOdometry();
    DriveSubsystem.getInstance().setAutoMode(true);
    autoCommand.initialize();
  }

  @Override
  public void execute() {
    autoCommand.execute();
  }

  @Override
  public boolean isFinished() {
    return autoCommand.isFinished();
  }

  @Override
  public void end(boolean interrupted) {
    autoCommand.end(interrupted);
    DriveSubsystem.getInstance().stop();
    DriveSubsystem.getInstance().setAutoMode(false);
  }
}