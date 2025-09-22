from flask import Flask, request, jsonify, Response
from firebase_admin import initialize_app, firestore
import tempfile
import subprocess
import os
import logging
import re
from jinja2 import Environment, FileSystemLoader
from datetime import datetime

# Initialize Flask app
app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO)

# Initialize Firebase Admin SDK
# The service will use Application Default Credentials in Cloud Run
try:
    initialize_app()
except ValueError:
    # App already initialized
    pass

# Jinja2 environment for LaTeX templates
template_env = Environment(
    loader=FileSystemLoader(os.path.join(os.path.dirname(__file__), 'templates')),
    block_start_string='<%',
    block_end_string='%>',
    variable_start_string='<<',
    variable_end_string='>>',
    comment_start_string='<#',
    comment_end_string='#>',
    trim_blocks=True,
    lstrip_blocks=True,
    autoescape=False
)

def escape_latex(text):
    """Escape special LaTeX characters in text."""
    if not isinstance(text, str):
        return text
    latex_special_chars = {
        '&': r'\\&', '%': r'\\%', '$': r'\\$', '#': r'\\#',
        '^': r'\\textasciicircum{}', '_': r'\\_', '{': r'\\{', '}': r'\\}',
        '~': r'\\textasciitilde{}', '\\': r'\\textbackslash{}',
    }
    escaped_text = text
    for char, escaped_char in latex_special_chars.items():
        escaped_text = escaped_text.replace(char, escaped_char)
    return escaped_text

def sanitize_text_list(text, delimiter=','):
    """Convert a delimited string to a list of escaped LaTeX strings."""
    if not text:
        return []
    items = [item.strip() for item in text.split(delimiter) if item.strip()]
    return [escape_latex(item) for item in items]

def format_date(date_str):
    """Format various date formats to a consistent format."""
    if not date_str:
        return ""
    date_str = str(date_str)
    
    # Handle GMT format
    if "GMT" in date_str:
        try:
            dt = datetime.strptime(date_str, "%a, %d %b %Y %H:%M:%S %Z")
            return dt.strftime("%b %Y")
        except:
            pass
    
    # Handle ISO format with timezone
    if ":" in date_str and ("-" in date_str or "+" in date_str):
        try:
            if "+" in date_str:
                dt = datetime.fromisoformat(date_str.replace('+00:00', ''))
            else:
                dt = datetime.fromisoformat(date_str.split('+')[0])
            return dt.strftime("%b %Y")
        except:
            pass
    
    # Handle 4-digit year
    if date_str.isdigit() and len(date_str) == 4:
        return date_str
    
    # Handle short date strings
    if len(date_str) <= 8:
        return date_str
    
    # Try to extract year and month
    try:
        year_match = re.search(r'20\d{2}', date_str)
        if year_match:
            year = year_match.group()
            month_match = re.search(r'(\d{1,2})-(\d{1,2})', date_str)
            if month_match:
                month_num = int(month_match.group(2)) if '-' in date_str else int(month_match.group(1))
                if 1 <= month_num <= 12:
                    month_names = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                                 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
                    return f"{month_names[month_num-1]} {year}"
            return year
    except:
        pass
    
    return str(date_str)

def clean_filename(filename):
    """Clean filename for use in file system and headers."""
    cleaned = re.sub(r'[<>:"/\\|?*]', '_', filename)
    cleaned = re.sub(r'\s+', ' ', cleaned.strip())
    if len(cleaned) > 50:
        cleaned = cleaned[:50]
    return cleaned or "resume"

def validate_resume_data(data):
    """Validate and format resume data."""
    validated_data = {}
    
    # Basic info
    validated_data['resume_name'] = escape_latex(data.get('resumeName', 'Resume'))
    validated_data['fullName'] = escape_latex(data.get('fullName', 'Your Name'))
    validated_data['phone'] = data.get('phone', '')
    validated_data['email'] = data.get('email', '')
    validated_data['website1'] = data.get('website1', '')
    validated_data['website2'] = data.get('website2', '')
    
    # Work experience
    work_experience = []
    for exp in data.get('experienceList', []):
        work_exp = {
            'title': escape_latex(exp.get('title', '')),
            'company': escape_latex(exp.get('company', '')),
            'location': escape_latex(exp.get('location', '')),
            'startDate': format_date(exp.get('startDate', '')),
            'endDate': format_date(exp.get('endDate', '')),
            'currentlyWorking': exp.get('currentlyWorking', False),
            'bullets': [escape_latex(b) for b in exp.get('bullets', [])] if exp.get('bullets', []) else []
        }
        work_experience.append(work_exp)
    validated_data['work_experience'] = work_experience
    
    # Education
    education = []
    for edu in data.get('educationList', []):
        edu_item = {
            'institution': escape_latex(edu.get('institution', '')),
            'location': escape_latex(edu.get('location', '')),
            'degree': escape_latex(edu.get('degree', '')),
            'major': escape_latex(edu.get('major', '')),
            'minor': escape_latex(edu.get('minor', '')),
            'gpa': edu.get('gpa', ''),
            'specialization': escape_latex(edu.get('specialization', '')),
            'startDate': format_date(edu.get('startDate', '')),
            'endDate': format_date(edu.get('endDate', '')),
        }
        education.append(edu_item)
    validated_data['education'] = education
    
    # Projects
    projects = []
    for proj in data.get('projectList', []):
        project = {
            'title': escape_latex(proj.get('title', '')),
            'stack': sanitize_text_list(proj.get('stack', '')),
            'date': proj.get('date', ''),
            'bullets': [escape_latex(b) for b in proj.get('bullets', [])] if proj.get('bullets', []) else []
        }
        projects.append(project)
    validated_data['projects'] = projects
    
    # Skills
    skills_data = data.get('skills', {})
    skills = {
        'languages': sanitize_text_list(skills_data.get('languages', '')),
        'frameworks': sanitize_text_list(skills_data.get('frameworks', '')),
        'tools': sanitize_text_list(skills_data.get('tools', '')),
        'libraries': sanitize_text_list(skills_data.get('libraries', ''))
    }
    validated_data['skills'] = skills
    
    # Template
    validated_data['template_id'] = data.get('templateName', data.get('templateId', 'template1')).lower()
    
    return validated_data

def get_resume_data(user_id, resume_id):
    """Fetch resume data from Firestore."""
    try:
        db = firestore.client()
        resume_ref = db.collection('users').document(user_id).collection('resumes').document(resume_id)
        resume_doc = resume_ref.get()
        if not resume_doc.exists:
            return None
        return resume_doc.to_dict()
    except Exception as e:
        logging.error(f"Error fetching resume data: {e}")
        return None

def format_data_for_template(resume_data):
    """Format resume data for template."""
    try:
        return validate_resume_data(resume_data)
    except Exception as e:
        logging.error(f"Error formatting data: {e}")
        return None

def generate_latex(template_name, data):
    """Generate LaTeX code from template and data."""
    try:
        template = template_env.get_template(f'{template_name}.tex')
        return template.render(**data)
    except Exception as e:
        logging.error(f"Error generating LaTeX: {e}")
        return None

def compile_latex_to_pdf(latex_code):
    """Compile LaTeX code to PDF."""
    try:
        with tempfile.TemporaryDirectory() as temp_dir:
            tex_file = os.path.join(temp_dir, 'resume.tex')
            with open(tex_file, 'w', encoding='utf-8') as f:
                f.write(latex_code)
            
            # Run pdflatex
            cmd = ['pdflatex', '-interaction=nonstopmode', '-output-directory', temp_dir, tex_file]
            result = subprocess.run(cmd, capture_output=True, text=True, cwd=temp_dir)
            
            pdf_file = os.path.join(temp_dir, 'resume.pdf')
            if not os.path.exists(pdf_file):
                logging.error(f"LaTeX compilation failed: {result.stderr}")
                return None
            
            with open(pdf_file, 'rb') as f:
                pdf_content = f.read()
            return pdf_content
    except Exception as e:
        logging.error(f"Error compiling LaTeX: {e}")
        return None

@app.route('/generate-resume', methods=['POST'])
def generate_resume():
    """Generate resume PDF endpoint."""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400
            
        user_id = data.get('user_id')
        resume_id = data.get('resume_id')
        
        if not user_id or not resume_id:
            return jsonify({'error': 'user_id and resume_id are required'}), 400
        
        # Fetch resume data
        resume_data = get_resume_data(user_id, resume_id)
        if not resume_data:
            return jsonify({'error': 'Resume not found'}), 404
        
        # Format data for template
        formatted_data = format_data_for_template(resume_data)
        if not formatted_data:
            return jsonify({'error': 'Failed to format resume data'}), 500
        
        # Select template
        template_map = {
            'template1': 'template1',
            'template2': 'template2',
            'template3': 'template3'
        }
        template_name = template_map.get(formatted_data['template_id'], 'template1')
        
        # Generate LaTeX
        latex_code = generate_latex(template_name, formatted_data)
        if not latex_code:
            return jsonify({'error': 'Failed to generate LaTeX'}), 500
        
        # Compile to PDF
        pdf_content = compile_latex_to_pdf(latex_code)
        if not pdf_content:
            return jsonify({'error': 'Failed to compile PDF'}), 500
        
        # Prepare response
        resume_name = formatted_data.get('resume_name', 'Resume')
        clean_name = clean_filename(resume_name)
        
        response = Response(
            pdf_content,
            mimetype='application/pdf',
            headers={
                'Content-Disposition': f'attachment; filename="{clean_name}.pdf"',
                'Content-Type': 'application/pdf'
            }
        )
        return response
        
    except Exception as e:
        logging.error(f"Error generating resume: {e}")
        return jsonify({'error': 'Internal server error'}), 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port, debug=False)
