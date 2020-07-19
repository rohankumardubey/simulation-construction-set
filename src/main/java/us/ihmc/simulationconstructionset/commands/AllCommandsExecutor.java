package us.ihmc.simulationconstructionset.commands;

import us.ihmc.simulationconstructionset.gui.dialogConstructors.GUIEnablerAndDisabler;
import us.ihmc.yoVariables.dataBuffer.DataBufferCommandsExecutor;
import us.ihmc.yoVariables.dataBuffer.KeyPointsHolder;

public interface AllCommandsExecutor extends DataBufferCommandsExecutor, RunCommandsExecutor, AddCameraKeyCommandExecutor, AddKeyPointCommandExecutor,
      CreateNewGraphWindowCommandExecutor, CreateNewViewportWindowCommandExecutor, CropBufferCommandExecutor, PackBufferCommandExecutor,
      CutBufferCommandExecutor, ThinBufferCommandExecutor, NextCameraKeyCommandExecutor, PreviousCameraKeyCommandExecutor, RemoveCameraKeyCommandExecutor,
      SelectGUIConfigFromFileCommandExecutor, SetInPointCommandExecutor, SetOutPointCommandExecutor, StepBackwardCommandExecutor, StepForwardCommandExecutor,
      ToggleCameraKeyModeCommandExecutor, KeyPointsHolder, ViewportSelectorCommandExecutor, ZoomGraphCommandExecutor,
      ExportSnapshotCommandExecutor, GUIEnablerAndDisabler, CreateNewYoVariableSliderWindowCommandExecutor
{
}
