require 'builder'

module PomTask
  class Config
    def enabled?
      !artifacts.nil? && !artifacts.empty?
    end

    def initialize(project)
      @project = project
    end

    attr_writer :artifacts
    def artifacts
      @artifacts ||= []
    end
  end

  class PomBuilder
    attr_reader :artifact
    attr_reader :dependencies

    def initialize(artifact, dependencies)
      @artifact = artifact
      @dependencies = dependencies
      @buffer = ""
      build
    end

    def build
      artifact_spec = artifact.to_hash
      xml = Builder::XmlMarkup.new(:target => @buffer, :indent => 2)
      xml.instruct!
      xml.project(
        "xsi:schemaLocation" => "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd",
        "xmlns" => "http://maven.apache.org/POM/4.0.0",
        "xmlns:xsi" => "http://www.w3.org/2001/XMLSchema-instance"
      ) do
        xml.modelVersion("4.0.0")
        xml.groupId(artifact_spec[:group])
        xml.artifactId(artifact_spec[:id])
        xml.version(artifact_spec[:version])

        version_properties = {}

        # Manage version numbers in a properties section
        xml.properties do
          dependencies.each do |dep|
            h = dep.to_hash
            prop_name = "#{h[:group]}-#{h[:id]}.version"
            xml.tag!(prop_name, h[:version])
            version_properties[h] = "${#{prop_name}}"
          end
        end

        xml.dependencies do
          dependencies.each do |dep|
            h = dep.to_hash
            xml.dependency do
              xml.groupId(h[:group])
              xml.artifactId(h[:id])
              xml.version(version_properties[h])
            end
          end
        end
      end
    end

    def write_pom(destination)
      FileUtils.mkdir_p(File.dirname(destination))
      File.open(destination, "w") { |f| f.write(@buffer) }
    end
  end

  module ProjectExtension
    include Extension

    def pom
      @pom ||= PomTask::Config.new(project)
    end

    first_time do
      desc 'Generate a POM file'
      Project.local_task('pom')
    end

    after_define do |project|
      pom = project.pom
      if pom.enabled?
        project.recursive_task('pom') do
          # Filter out Rake::FileTask dependencies.  Note use of instance_of? since
          # Buildr::Artifact and JarTask are subclasses of FileTask.
          deps = project.compile.dependencies.reject { |dep| dep.instance_of?(Rake::FileTask) }
          pom.artifacts.each do |artifact|
            spec = artifact.to_hash
            destination = project.path_to("pom.xml")

            # Special case for when we want to build a POM for just candlepin-api.jar
            if pom.artifacts.length > 1 && spec[:type] != :war
              destination = project.path_to(:target, "#{spec[:id]}-#{spec[:version]}.pom")
            end

            xml = PomBuilder.new(artifact, deps)
            xml.write_pom(destination)
            info("POM written to #{destination}")
          end
        end
      end
    end
  end
end

class Buildr::Project
  include PomTask::ProjectExtension
end
