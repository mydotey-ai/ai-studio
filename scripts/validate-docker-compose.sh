#!/bin/bash
# Docker Compose Configuration Validation Script

set -e

echo "=========================================="
echo "Docker Compose Configuration Validation"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}Error: Python 3 is required for YAML validation${NC}"
    exit 1
fi

# Function to validate YAML file
validate_yaml() {
    local file=$1
    echo -n "Validating $file... "

    if python3 -c "import yaml; yaml.safe_load(open('$file'))" 2>/dev/null; then
        echo -e "${GREEN}✓ Valid${NC}"
        return 0
    else
        echo -e "${RED}✗ Invalid${NC}"
        return 1
    fi
}

# Function to check if file exists
check_file() {
    local file=$1
    echo -n "Checking $file... "

    if [ -f "$file" ]; then
        echo -e "${GREEN}✓ Exists${NC}"
        return 0
    else
        echo -e "${RED}✗ Missing${NC}"
        return 1
    fi
}

# Validation counter
VALIDATED=0
FAILED=0

echo "Checking required files..."
echo "----------------------------"

# Check Docker Compose files
check_file "docker-compose.yml" && ((VALIDATED++)) || ((FAILED++))
check_file "docker-compose.dev.yml" && ((VALIDATED++)) || ((FAILED++))
check_file "docker-compose.prod.yml" && ((VALIDATED++)) || ((FAILED++))
check_file ".env.example" && ((VALIDATED++)) || ((FAILED++))

echo ""
echo "Validating YAML syntax..."
echo "----------------------------"

# Validate YAML files
validate_yaml "docker-compose.yml" && ((VALIDATED++)) || ((FAILED++))
validate_yaml "docker-compose.dev.yml" && ((VALIDATED++)) || ((FAILED++))
validate_yaml "docker-compose.prod.yml" && ((VALIDATED++)) || ((FAILED++))

# Check Prometheus configuration
if [ -d "prometheus" ]; then
    echo ""
    echo "Checking Prometheus configuration..."
    echo "----------------------------"

    check_file "prometheus/prometheus.yml" && ((VALIDATED++)) || ((FAILED++))
    check_file "prometheus/prometheus.dev.yml" && ((VALIDATED++)) || ((FAILED++))
    check_file "prometheus/prometheus.prod.yml" && ((VALIDATED++)) || ((FAILED++))

    validate_yaml "prometheus/prometheus.yml" && ((VALIDATED++)) || ((FAILED++))
    validate_yaml "prometheus/prometheus.dev.yml" && ((VALIDATED++)) || ((FAILED++))
    validate_yaml "prometheus/prometheus.prod.yml" && ((VALIDATED++)) || ((FAILED++))
fi

# Check Grafana configuration
if [ -d "grafana" ]; then
    echo ""
    echo "Checking Grafana configuration..."
    echo "----------------------------"

    check_file "grafana/provisioning/datasources/prometheus.yml" && ((VALIDATED++)) || ((FAILED++))
    validate_yaml "grafana/provisioning/datasources/prometheus.yml" && ((VALIDATED++)) || ((FAILED++))
fi

echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All validations passed! ($VALIDATED checks)${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Copy .env.example to .env and configure it"
    echo "2. Run: docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d"
    exit 0
else
    echo -e "${RED}Validation failed!${NC}"
    echo -e "Passed: ${GREEN}$VALIDATED${NC}"
    echo -e "Failed: ${RED}$FAILED${NC}"
    exit 1
fi
