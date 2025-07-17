import os

def collect_files(project_directory, output_file, extensions=None):
    if extensions is None:
        extensions = ['.java', '.json', '.fxml', '.css', '.svg', '.xml']  # Добавлено расширение .xml

    with open(output_file, 'w', encoding='utf-8') as output:
        for root, dirs, files in os.walk(project_directory):
            for file in files:
                # Захватываем только файлы с указанными расширениями
                if any(file.endswith(ext) for ext in extensions):
                    file_path = os.path.join(root, file)
                    output.write(f"// {file_path}\n")
                    with open(file_path, 'r', encoding='utf-8') as input_file:
                        output.write(input_file.read())
                    output.write("\n\n")

# Укажите путь к вашему проекту
project_dir = r"K:\Programm\IDEA Project\KoverG\KoverG"  # Замените на путь к вашему проекту
output_file_path = "combined_project_files.java"  # Название выходного файла

# Вызываем функцию сбора всех файлов
collect_files(project_dir, output_file_path)
print(f"Скрипт завершен! Все файлы собраны в файл {output_file_path}")
