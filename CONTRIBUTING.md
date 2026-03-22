# Contributing to Eden M-Kopa

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/eden-mkopa.git`
3. Create a branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Test thoroughly
6. Commit: `git commit -m "Add your feature"`
7. Push: `git push origin feature/your-feature-name`
8. Create a Pull Request

## Development Setup

### Prerequisites
- Node.js 18+
- Python 3.11+
- Android Studio
- Supabase account

### Local Development

1. Database:
   ```bash
   # Set up Supabase project and run SQL scripts
   ```

2. Dashboard:
   ```bash
   cd dashboard
   npm install
   cp .env.local.example .env.local
   # Add your Supabase credentials
   npm run dev
   ```

3. Backend:
   ```bash
   cd backend
   python -m venv venv
   source venv/bin/activate  # or venv\Scripts\activate on Windows
   pip install -r requirements.txt
   cp .env.example .env
   # Add your credentials
   python main.py
   ```

4. Android:
   ```bash
   # Open android/ folder in Android Studio
   # Sync Gradle
   # Run on emulator or device
   ```

## Code Style

### TypeScript/JavaScript
- Use TypeScript for all new code
- Follow Airbnb style guide
- Use Prettier for formatting
- Run `npm run lint` before committing

### Python
- Follow PEP 8
- Use type hints
- Run `black` for formatting
- Run `pylint` before committing

### Kotlin
- Follow Kotlin coding conventions
- Use Android Studio formatter
- Keep functions small and focused

## Testing

- Write tests for new features
- Ensure all tests pass before submitting PR
- Test on multiple devices/browsers

## Pull Request Guidelines

1. Update documentation if needed
2. Add tests for new features
3. Ensure CI passes
4. Keep PRs focused and small
5. Write clear commit messages

## Reporting Issues

- Use GitHub Issues
- Provide detailed description
- Include steps to reproduce
- Add screenshots if applicable
- Specify environment (OS, browser, etc.)

## Feature Requests

- Open a GitHub Issue
- Describe the feature clearly
- Explain the use case
- Discuss before implementing

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers
- Focus on constructive feedback
- No harassment or discrimination

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
