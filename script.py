import os
import logging
import subprocess
from datetime import datetime

# Функция для настройки логирования
def setup_logging(output_directory):
    service_directory = os.path.join(output_directory, "Service")

    if not os.path.exists(service_directory):
        os.makedirs(service_directory)
        logging.info(f"Created 'Service' directory at {service_directory}")

    log_file_path = os.path.join(service_directory, "script_log.txt")

    if os.path.exists(log_file_path):
        with open(log_file_path, 'w', encoding='utf-8') as log_file:
            log_file.truncate(0)

    file_handler = logging.FileHandler(log_file_path, 'a', 'utf-8')
    console_handler = logging.StreamHandler()

    log_format = '%(asctime)s - %(levelname)s - %(message)s'

    logging.basicConfig(
        level=logging.INFO,
        format=log_format,
        handlers=[file_handler, console_handler]
    )

def collect_files(project_directory, output_directory, extensions=None):
    if extensions is None:
        extensions = ['.java', '.json', '.fxml', '.css', '.xml']

    current_date = datetime.now().strftime("%Y-%m-%d")
    logging.info(f"Current date: {current_date}")

    base_output_file = f"combined_project_files_{current_date}"
    version = 1
    service_directory = os.path.join(output_directory, "Service")
    version_file_path = os.path.join(service_directory, "versions.txt")

    if not os.path.exists(service_directory):
        os.makedirs(service_directory)
        logging.info(f"Created 'Service' directory at {service_directory}")

    if os.path.exists(version_file_path):
        logging.info(f"Found versions.txt, processing existing versions.")
        with open(version_file_path, 'r', encoding='utf-8') as version_file:
            versions = version_file.readlines()
            updated_versions = []
            versions_exist = False

            for line in versions:
                date, last_version = line.strip().split(',')
                file_path = os.path.join(output_directory, f"{base_output_file}_v{last_version}.java")

                if os.path.exists(file_path):
                    updated_versions.append(line)
                    if date == current_date:
                        version = int(last_version) + 1
                        versions_exist = True

            with open(version_file_path, 'w', encoding='utf-8') as version_file:
                version_file.writelines(updated_versions)
                logging.info(f"Updated versions in {version_file_path}. Removed outdated versions.")
    else:
        logging.info(f"{version_file_path} does not exist. Creating a new file.")
        with open(version_file_path, 'w', encoding='utf-8') as version_file:
            logging.info(f"Created new versions.txt at {version_file_path}.")
        versions_exist = False

    if not versions_exist:
        logging.info(f"No previous version found for {current_date}. Starting with version 1.")

    output_file = os.path.join(output_directory, f"{base_output_file}_v{version}.java")
    logging.info(f"Creating output file: {output_file}")

    with open(version_file_path, 'a', encoding='utf-8') as version_file:
        version_file.write(f"{current_date},{version}\n")
        logging.info(f"Added new version {version} for {current_date} to {version_file_path}.")

    txt_output_file = os.path.join(output_directory, f"{base_output_file}_v{version}.txt")

    try:
        collected_files = set()  # Set to store unique (path, content) pairs
        with open(output_file, 'w', encoding='utf-8') as java_output, open(txt_output_file, 'w', encoding='utf-8') as txt_output:
            logging.info(f"Collecting files from {project_directory}...")

            # Process directories and files
            for root, dirs, files in os.walk(project_directory):
                if '.idea' in root or 'target' in root:
                    continue  # Skip these directories

                for file in files:
                    file_path = os.path.join(root, file)
                    # Process files with the specified extensions
                    if any(file.endswith(ext) for ext in extensions):
                        with open(file_path, 'r', encoding='utf-8') as input_file:
                            file_content = input_file.read()

                        # Create a unique pair (path, content)
                        file_pair = (file_path, file_content)

                        if file_pair not in collected_files:
                            collected_files.add(file_pair)
                            java_output.write(f"// {file_path}\n")
                            java_output.write(file_content)
                            java_output.write("\n\n")

                            txt_output.write(f"// {file_path}\n")
                            txt_output.write(file_content)
                            txt_output.write("\n\n")

                            # Log only when the file is written
                            logging.info(f"File {file_path} has been successfully written to output.")

        logging.info(f"File {output_file} and {txt_output_file} have been successfully saved!")
        open_folder(output_file)
    except Exception as e:
        logging.error(f"Error while saving the file {output_file}: {e}")

def open_folder(file_path):
    folder_path = os.path.dirname(file_path)
    if os.name == 'nt':
        subprocess.run(['explorer', folder_path])
    elif os.name == 'posix':
        subprocess.run(['open', folder_path])
    elif os.name == 'posix':
        subprocess.run(['xdg-open', folder_path])

project_dir = r"K:\Programm\IDEA Project\KoverG\KoverG"
output_dir = r"K:\Programm\IDEA Project\KoverG\Version"

setup_logging(output_dir)

collect_files(project_dir, output_dir)
