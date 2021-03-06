package us.ihmc.simulationconstructionset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import us.ihmc.euclid.matrix.interfaces.Matrix3DReadOnly;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.robotics.robotDescription.CameraSensorDescription;
import us.ihmc.robotics.robotDescription.CollisionMeshDescription;
import us.ihmc.robotics.robotDescription.ExternalForcePointDescription;
import us.ihmc.robotics.robotDescription.FloatingJointDescription;
import us.ihmc.robotics.robotDescription.FloatingPlanarJointDescription;
import us.ihmc.robotics.robotDescription.ForceSensorDescription;
import us.ihmc.robotics.robotDescription.GroundContactPointDescription;
import us.ihmc.robotics.robotDescription.IMUSensorDescription;
import us.ihmc.robotics.robotDescription.JointDescription;
import us.ihmc.robotics.robotDescription.JointWrenchSensorDescription;
import us.ihmc.robotics.robotDescription.KinematicPointDescription;
import us.ihmc.robotics.robotDescription.LidarSensorDescription;
import us.ihmc.robotics.robotDescription.LinkDescription;
import us.ihmc.robotics.robotDescription.LinkGraphicsDescription;
import us.ihmc.robotics.robotDescription.LoopClosureConstraintDescription;
import us.ihmc.robotics.robotDescription.PinJointDescription;
import us.ihmc.robotics.robotDescription.RobotDescription;
import us.ihmc.robotics.robotDescription.SliderJointDescription;
import us.ihmc.simulationconstructionset.simulatedSensors.CollisionShapeBasedWrenchCalculator;
import us.ihmc.simulationconstructionset.simulatedSensors.FeatherStoneJointBasedWrenchCalculator;
import us.ihmc.simulationconstructionset.simulatedSensors.GroundContactPointBasedWrenchCalculator;
import us.ihmc.simulationconstructionset.simulatedSensors.LidarMount;
import us.ihmc.simulationconstructionset.simulatedSensors.WrenchCalculatorInterface;

public class RobotFromDescription extends Robot implements OneDegreeOfFreedomJointHolder
{
   private final Map<String, Joint> jointNameMap = new LinkedHashMap<>();
   private final Map<JointDescription, Joint> jointDescriptionMap = new LinkedHashMap<>();
   private final Map<LinkDescription, Link> linkDescriptionMap = new LinkedHashMap<>();

   private final Map<String, OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new LinkedHashMap<>();

   private final Map<String, CameraMount> cameraNameMap = new LinkedHashMap<>();
   private final Map<CameraSensorDescription, CameraMount> cameraDescriptionMap = new LinkedHashMap<>();

   private final Map<String, LidarMount> lidarNameMap = new LinkedHashMap<>();
   private final Map<LidarSensorDescription, LidarMount> lidarDescriptionMap = new LinkedHashMap<>();

   private final Map<String, IMUMount> imuNameMap = new LinkedHashMap<>();
   private final Map<IMUSensorDescription, IMUMount> imuDescriptionMap = new LinkedHashMap<>();

   private final Map<String, JointWrenchSensor> wrenchSensorNameMap = new LinkedHashMap<>();
   private final Map<JointWrenchSensorDescription, JointWrenchSensor> wrenchSensorDescriptionMap = new LinkedHashMap<>();

   private final Map<Joint, ArrayList<GroundContactPoint>> jointToGroundContactPointsMap = new LinkedHashMap<>();

   public RobotFromDescription(RobotDescription description)
   {
      this(description, true, true);
   }

   public RobotFromDescription(RobotDescription description, boolean enableDamping, boolean enableJointTorqueAndVelocityLimits)
   {
      super(description.getName());
      constructRobotFromDescription(description, enableDamping, enableJointTorqueAndVelocityLimits);
   }

   @Override
   public Joint getJoint(String jointName)
   {
      return jointNameMap.get(jointName);
   }

   public Joint getJoint(JointDescription jointDescription)
   {
      return jointDescriptionMap.get(jointDescription);
   }

   @Override
   public OneDegreeOfFreedomJoint getOneDegreeOfFreedomJoint(String name)
   {
      return oneDegreeOfFreedomJoints.get(name);
   }

   public OneDegreeOfFreedomJoint[] getOneDegreeOfFreedomJoints()
   {
      return oneDegreeOfFreedomJoints.values().toArray(new OneDegreeOfFreedomJoint[oneDegreeOfFreedomJoints.size()]);
   }

   public CameraMount getCameraMount(String cameraName)
   {
      return cameraNameMap.get(cameraName);
   }

   public CameraMount getCameraMount(CameraSensorDescription cameraSensorDescription)
   {
      return cameraDescriptionMap.get(cameraSensorDescription);
   }

   public IMUMount getIMUMount(String name)
   {
      return imuNameMap.get(name);
   }

   public IMUMount getIMUMount(IMUSensorDescription imuSensorDescription)
   {
      return imuDescriptionMap.get(imuSensorDescription);
   }

   public JointWrenchSensor getJointWrenchSensor(String name)
   {
      return wrenchSensorNameMap.get(name);
   }

   public JointWrenchSensor getJointWrenchSensor(JointWrenchSensorDescription jointWrenchSensorDescription)
   {
      return wrenchSensorDescriptionMap.get(jointWrenchSensorDescription);
   }

   public ArrayList<GroundContactPoint> getGroundContactPointsOnJoint(Joint joint)
   {
      return jointToGroundContactPointsMap.get(joint);
   }

   private void constructRobotFromDescription(RobotDescription description, boolean enableDamping, boolean enableJointTorqueAndVelocityLimits)
   {
      List<JointDescription> rootJointDescriptions = description.getRootJoints();

      for (JointDescription rootJointDescription : rootJointDescriptions)
      {
         Joint rootJoint = constructJointRecursively(rootJointDescription, enableDamping, enableJointTorqueAndVelocityLimits);
         addRootJoint(rootJoint);
      }

      for (JointDescription rootJointDescription : rootJointDescriptions)
      {
         addLoopClosureConstraintsRecursively(rootJointDescription);
      }

      for (JointDescription rootJointDescription : rootJointDescriptions)
      {
         addForceSensorRecursively(rootJointDescription);
      }
   }

   private Joint constructJointRecursively(JointDescription jointDescription, boolean enableDamping, boolean enableJointTorqueAndVelocityLimits)
   {
      Joint joint = createSingleJoint(jointDescription, enableDamping, enableJointTorqueAndVelocityLimits);

      addGroundContactPoints(jointDescription, joint);
      addExternalForcePoints(jointDescription, joint);
      addKinematicPoints(jointDescription, joint);

      addExternalForcePointsFromCollisionMesh(jointDescription, joint);

      addLidarMounts(jointDescription, joint);
      addCameraMounts(jointDescription, joint);
      addIMUMounts(jointDescription, joint);
      addJointWrenchSensors(jointDescription, joint);

      //addForceSensors(jointDescription, joint);

      // Iterate over the children
      List<JointDescription> childrenJoints = jointDescription.getChildrenJoints();
      for (JointDescription childJointDescription : childrenJoints)
      {
         Joint childJoint = constructJointRecursively(childJointDescription, enableDamping, enableJointTorqueAndVelocityLimits);
         joint.addJoint(childJoint);
      }

      jointNameMap.put(joint.getName(), joint);
      jointDescriptionMap.put(jointDescription, joint);
      linkDescriptionMap.put(jointDescription.getLink(), joint.getLink());

      if (joint instanceof OneDegreeOfFreedomJoint)
      {
         oneDegreeOfFreedomJoints.put(joint.getName(), (OneDegreeOfFreedomJoint) joint);
      }

      return joint;
   }

   private void addForceSensorRecursively(JointDescription jointDescription)
   {
      Joint joint = jointNameMap.get(jointDescription.getName());

      List<ForceSensorDescription> forceSensorDescriptions = jointDescription.getForceSensors();

      for (ForceSensorDescription forceSensorDescription : forceSensorDescriptions)
      {
         WrenchCalculatorInterface wrenchCalculator;

         if (forceSensorDescription.useGroundContactPoints())
         {
            ArrayList<GroundContactPoint> groundContactPoints = new ArrayList<>();
            joint.recursiveGetAllGroundContactPoints(groundContactPoints);

            wrenchCalculator = new GroundContactPointBasedWrenchCalculator(forceSensorDescription.getName(),
                                                                           groundContactPoints,
                                                                           joint,
                                                                           forceSensorDescription.getTransformToJoint(),
                                                                           yoRegistry);

            if (forceSensorDescription.useShapeCollision())
            {
               List<ExternalForcePoint> contactPoints = new ArrayList<>();
               contactPoints = joint.getExternalForcePoints();
               wrenchCalculator = new CollisionShapeBasedWrenchCalculator(forceSensorDescription.getName(),
                                                                          contactPoints,
                                                                          joint,
                                                                          forceSensorDescription.getTransformToJoint(),
                                                                          yoRegistry);
            }
         }
         else
         {
            Vector3D offsetToPack = new Vector3D();
            offsetToPack.set(forceSensorDescription.getTransformToJoint().getTranslation());
            JointWrenchSensor jointWrenchSensor = new JointWrenchSensor(forceSensorDescription.getName(), offsetToPack, this);
            joint.addJointWrenchSensor(jointWrenchSensor);

            wrenchCalculator = new FeatherStoneJointBasedWrenchCalculator(forceSensorDescription.getName(), joint);
         }

         joint.addForceSensor(wrenchCalculator);
      }

      List<JointDescription> childrenJoints = jointDescription.getChildrenJoints();

      for (JointDescription childJointDescription : childrenJoints)
         addForceSensorRecursively(childJointDescription);
   }

   private void addLoopClosureConstraintsRecursively(JointDescription jointDescription)
   {
      Joint joint = jointNameMap.get(jointDescription.getName());

      List<LoopClosureConstraintDescription> constraintDescriptions = jointDescription.getChildrenConstraintDescriptions();

      for (LoopClosureConstraintDescription constraintDescription : constraintDescriptions)
      {
         String name = constraintDescription.getName();
         Tuple3DReadOnly offsetFromParentJoint = constraintDescription.getOffsetFromParentJoint();
         Tuple3DReadOnly offsetFromLinkParentJoint = constraintDescription.getOffsetFromLinkParentJoint();
         Matrix3DReadOnly constraintForceSubSpace = constraintDescription.getConstraintForceSubSpace();
         Matrix3DReadOnly constraintMomentSubSpace = constraintDescription.getConstraintMomentSubSpace();
         LoopClosureSoftConstraint constraint = new LoopClosureSoftConstraint(name,
                                                                              offsetFromParentJoint,
                                                                              offsetFromLinkParentJoint,
                                                                              this,
                                                                              constraintForceSubSpace,
                                                                              constraintMomentSubSpace);
         constraint.setGains(constraintDescription.getProportionalGains(), constraintDescription.getDerivativeGains());

         joint.addLoopClosureConstraint(constraint);
         Link link = linkDescriptionMap.get(constraintDescription.getLink());
         Objects.requireNonNull(link, "Could not find link: " + constraintDescription.getLink().getName());
         constraint.setLink(link);
      }

      for (JointDescription childJointDescription : jointDescription.getChildrenJoints())
      {
         addLoopClosureConstraintsRecursively(childJointDescription);
      }
   }

   private void addExternalForcePointsFromCollisionMesh(JointDescription jointDescription, Joint joint)
   {
      Link link = joint.getLink();
      List<CollisionMeshDescription> collisionMeshDescriptions = link.getCollisionMeshDescriptions();

      if (collisionMeshDescriptions != null)
      {
         int estimatedNumberOfContactPoints = 0;

         for (int i = 0; i < collisionMeshDescriptions.size(); i++)
         {
            CollisionMeshDescription collisionMesh = collisionMeshDescriptions.get(i);
            estimatedNumberOfContactPoints += collisionMesh.getEstimatedNumberOfContactPoints();
         }

         link.enableContactingExternalForcePoints(estimatedNumberOfContactPoints, yoRegistry);
      }
   }

   private void addLidarMounts(JointDescription jointDescription, Joint joint)
   {
      List<LidarSensorDescription> lidarSensorDescriptions = jointDescription.getLidarSensors();

      for (LidarSensorDescription lidarSensorDescription : lidarSensorDescriptions)
      {
         LidarMount lidarMount = new LidarMount(lidarSensorDescription);
         joint.addLidarMount(lidarMount);

         //TODO: Should we really call addSensor here?
         // Instead, perhaps, there should be a better way to get the sensors from a robot...
         joint.addSensor(lidarMount);

         lidarNameMap.put(lidarMount.getName(), lidarMount);
         lidarDescriptionMap.put(lidarSensorDescription, lidarMount);
      }
   }

   private void addCameraMounts(JointDescription jointDescription, Joint joint)
   {
      List<CameraSensorDescription> cameraSensorDescriptions = jointDescription.getCameraSensors();
      for (CameraSensorDescription cameraSensorDescription : cameraSensorDescriptions)
      {
         CameraMount cameraMount = new CameraMount(cameraSensorDescription.getName(),
                                                   cameraSensorDescription.getTransformToJoint(),
                                                   cameraSensorDescription.getFieldOfView(),
                                                   cameraSensorDescription.getClipNear(),
                                                   cameraSensorDescription.getClipFar(),
                                                   this);
         cameraMount.setImageWidth(cameraSensorDescription.getImageWidth());
         cameraMount.setImageHeight(cameraSensorDescription.getImageHeight());

         joint.addCameraMount(cameraMount);

         cameraNameMap.put(cameraMount.getName(), cameraMount);
         cameraDescriptionMap.put(cameraSensorDescription, cameraMount);
      }
   }

   private void addIMUMounts(JointDescription jointDescription, Joint joint)
   {
      List<IMUSensorDescription> imuSensorDescriptions = jointDescription.getIMUSensors();
      for (IMUSensorDescription imuSensorDescription : imuSensorDescriptions)
      {
         IMUMount imuMount = new IMUMount(imuSensorDescription.getName(), imuSensorDescription.getTransformToJoint(), this);
         joint.addIMUMount(imuMount);

         imuNameMap.put(imuMount.getName(), imuMount);
         imuDescriptionMap.put(imuSensorDescription, imuMount);
      }
   }

   private void addJointWrenchSensors(JointDescription jointDescription, Joint joint)
   {
      List<JointWrenchSensorDescription> jointWrenchSensorDescriptions = jointDescription.getWrenchSensors();
      for (JointWrenchSensorDescription jointWrenchSensorDescription : jointWrenchSensorDescriptions)
      {
         JointWrenchSensor jointWrenchSensor = new JointWrenchSensor(jointWrenchSensorDescription.getName(),
                                                                     jointWrenchSensorDescription.getOffsetFromJoint(),
                                                                     this);
         joint.addJointWrenchSensor(jointWrenchSensor);

         wrenchSensorNameMap.put(jointWrenchSensor.getName(), jointWrenchSensor);
         wrenchSensorDescriptionMap.put(jointWrenchSensorDescription, jointWrenchSensor);
      }
   }

   private void addForceSensors(JointDescription jointDescription, Joint joint)
   {
      List<ForceSensorDescription> forceSensorDescriptions = jointDescription.getForceSensors();

      for (ForceSensorDescription forceSensorDescription : forceSensorDescriptions)
      {
         WrenchCalculatorInterface wrenchCalculator;

         if (forceSensorDescription.useGroundContactPoints())
         {
            //               System.out.println("SDFRobot: Adding old-school force sensor to: " + joint.getName());
            ArrayList<GroundContactPoint> groundContactPoints = new ArrayList<>();
            //TODO: Not sure if you want all of the ground contact points from here down, or just the ones attached to this joint.
            joint.recursiveGetAllGroundContactPoints(groundContactPoints);

            wrenchCalculator = new GroundContactPointBasedWrenchCalculator(forceSensorDescription.getName(),
                                                                           groundContactPoints,
                                                                           joint,
                                                                           forceSensorDescription.getTransformToJoint(),
                                                                           yoRegistry);
         }
         else
         {
            //               System.out.println("SDFRobot: Adding force sensor to: " + joint.getName());

            Vector3D offsetToPack = new Vector3D();
            offsetToPack.set(forceSensorDescription.getTransformToJoint().getTranslation());
            JointWrenchSensor jointWrenchSensor = new JointWrenchSensor(forceSensorDescription.getName(), offsetToPack, this);
            joint.addJointWrenchSensor(jointWrenchSensor);

            wrenchCalculator = new FeatherStoneJointBasedWrenchCalculator(forceSensorDescription.getName(), joint);
         }

         joint.addForceSensor(wrenchCalculator);
      }
   }

   private void addGroundContactPoints(JointDescription jointDescription, Joint joint)
   {
      List<GroundContactPointDescription> groundContactPointDescriptions = jointDescription.getGroundContactPoints();

      for (GroundContactPointDescription groundContactPointDescription : groundContactPointDescriptions)
      {
         GroundContactPoint groundContactPoint = new GroundContactPoint(groundContactPointDescription.getName(),
                                                                        groundContactPointDescription.getOffsetFromJoint(),
                                                                        this);
         joint.addGroundContactPoint(groundContactPointDescription.getGroupIdentifier(), groundContactPoint);

         if (!jointToGroundContactPointsMap.containsKey(joint))
         {
            jointToGroundContactPointsMap.put(joint, new ArrayList<GroundContactPoint>());
         }
         jointToGroundContactPointsMap.get(joint).add(groundContactPoint);
      }
   }

   private void addExternalForcePoints(JointDescription jointDescription, Joint joint)
   {
      List<ExternalForcePointDescription> ExternalForcePointDescriptions = jointDescription.getExternalForcePoints();

      for (ExternalForcePointDescription ExternalForcePointDescription : ExternalForcePointDescriptions)
      {
         ExternalForcePoint ExternalForcePoint = new ExternalForcePoint(ExternalForcePointDescription.getName(),
                                                                        ExternalForcePointDescription.getOffsetFromJoint(),
                                                                        this);
         joint.addExternalForcePoint(ExternalForcePoint);
      }
   }

   private void addKinematicPoints(JointDescription jointDescription, Joint joint)
   {
      List<KinematicPointDescription> KinematicPointDescriptions = jointDescription.getKinematicPoints();

      for (KinematicPointDescription KinematicPointDescription : KinematicPointDescriptions)
      {
         KinematicPoint KinematicPoint = new KinematicPoint(KinematicPointDescription.getName(), KinematicPointDescription.getOffsetFromJoint(), this);
         joint.addKinematicPoint(KinematicPoint);
      }
   }

   private Joint createSingleJoint(JointDescription jointDescription, boolean enableDamping, boolean enableJointTorqueAndVelocityLimits)
   {
      Joint joint;

      if (jointDescription instanceof FloatingJointDescription)
      {
         FloatingJointDescription floatingJointDescription = (FloatingJointDescription) jointDescription;

         Vector3D offset = new Vector3D();
         floatingJointDescription.getOffsetFromParentJoint(offset);

         joint = new FloatingJoint(jointDescription.getName(), floatingJointDescription.getJointVariableName(), offset, this, true);
      }

      else if (jointDescription instanceof FloatingPlanarJointDescription)
      {
         FloatingPlanarJointDescription floatingPlanarJointDescription = (FloatingPlanarJointDescription) jointDescription;

         joint = new FloatingPlanarJoint(jointDescription.getName(), this, floatingPlanarJointDescription.getPlane());
      }

      else if (jointDescription instanceof PinJointDescription)
      {
         PinJointDescription pinJointDescription = (PinJointDescription) jointDescription;
         Vector3D offset = new Vector3D();
         pinJointDescription.getOffsetFromParentJoint(offset);

         if (jointDescription.isDynamic())
         {
            Vector3D jointAxis = new Vector3D();
            pinJointDescription.getJointAxis(jointAxis);
            joint = new PinJoint(jointDescription.getName(), offset, this, jointAxis);

            PinJoint pinJoint = (PinJoint) joint;

            if (pinJointDescription.containsLimitStops())
            {
               double[] limitStopParameters = pinJointDescription.getLimitStopParameters();

               double qMin = limitStopParameters[0];
               double qMax = limitStopParameters[1];
               double kLimit = limitStopParameters[2];
               double bLimit = limitStopParameters[3];

               pinJoint.setLimitStops(qMin, qMax, kLimit, bLimit);
            }

            if (enableDamping)
            {
               pinJoint.setDamping(pinJointDescription.getDamping());
               pinJoint.setStiction(pinJointDescription.getStiction());
            }
            if (enableJointTorqueAndVelocityLimits)
            {
               pinJoint.setVelocityLimits(pinJointDescription.getVelocityLimit(), pinJointDescription.getVelocityDamping());
            }
         }
         else
         {
            Vector3D jointAxis = new Vector3D();
            pinJointDescription.getJointAxis(jointAxis);
            joint = new DummyOneDegreeOfFreedomJoint(jointDescription.getName(), offset, this, jointAxis);
         }
      }
      else if (jointDescription instanceof SliderJointDescription)
      {
         SliderJointDescription sliderJointDescription = (SliderJointDescription) jointDescription;
         Vector3D offset = new Vector3D();
         sliderJointDescription.getOffsetFromParentJoint(offset);

         Vector3D jointAxis = new Vector3D();
         sliderJointDescription.getJointAxis(jointAxis);
         joint = new SliderJoint(jointDescription.getName(), offset, this, jointAxis);

         SliderJoint sliderJoint = (SliderJoint) joint;

         if (sliderJointDescription.containsLimitStops())
         {
            double[] limitStopParameters = sliderJointDescription.getLimitStopParameters();

            double qMin = limitStopParameters[0];
            double qMax = limitStopParameters[1];
            double kLimit = limitStopParameters[2];
            double bLimit = limitStopParameters[3];

            sliderJoint.setLimitStops(qMin, qMax, kLimit, bLimit);
         }
      }

      else
      {
         throw new RuntimeException("Don't support that joint type yet. Please implement it! Type = " + jointDescription.getClass());
      }

      if (!jointDescription.isDynamic())
      {
         joint.setDynamic(false);
      }

      LinkDescription linkDescription = jointDescription.getLink();

      if (linkDescription == null)
      {
         throw new RuntimeException("LinkDescription is null for joint " + jointDescription.getName());
      }
      Link link = createLink(linkDescription);
      joint.setLink(link);
      return joint;
   }

   private Link createLink(LinkDescription linkDescription)
   {
      Link link = new Link(linkDescription.getName());

      link.setMass(linkDescription.getMass());
      link.setComOffset(linkDescription.getCenterOfMassOffset());
      link.setMomentOfInertia(linkDescription.getMomentOfInertia());

      LinkGraphicsDescription linkGraphics = linkDescription.getLinkGraphics();
      link.setLinkGraphics(linkGraphics);

      List<CollisionMeshDescription> collisonMeshDescriptions = linkDescription.getCollisionMeshes();

      for (int i = 0; i < collisonMeshDescriptions.size(); i++)
      {
         link.addCollisionMesh(collisonMeshDescriptions.get(i));
      }

      return link;
   }
}
