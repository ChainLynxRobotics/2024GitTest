// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

public class RobotContainer {

  public static final double GEAR_RATIO = 1.5;

  public static final double GEAR_RATIO2 = 1.5;
  public static final double GEAR_RATIO3 = 1.5;

  
  public RobotContainer() {
    configureBindings();
    // hey this is a comment

    // this is another

    final int blah = 3;
  }

  private void configureBindings() {}

  public Command getAutonomousCommand() {
    return Commands.print("No autonomous command configured");
  }
}
