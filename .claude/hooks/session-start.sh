#!/bin/bash
set -euo pipefail

# Claude Code 웹 환경에서만 실행
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "🚀 Starting Docker setup for TestContainers..."

# Docker가 이미 설치되어 있는지 확인
if command -v docker &> /dev/null; then
  echo "✅ Docker already installed: $(docker --version)"

  # Docker 데몬이 실행 중인지 확인
  if docker info &> /dev/null; then
    echo "✅ Docker daemon is already running"
    exit 0
  else
    echo "🔄 Docker daemon not running, starting..."
  fi
else
  echo "📦 Installing Docker..."

  # 문제가 되는 PPA 저장소 제거
  rm -f /etc/apt/sources.list.d/deadsnakes-ubuntu-ppa-noble.sources
  rm -f /etc/apt/sources.list.d/ondrej-ubuntu-php-noble.sources

  # /tmp 권한 수정
  chmod 1777 /tmp 2>/dev/null || true

  # apt 업데이트
  apt-get update -y -qq

  # Docker GPG 키 및 저장소 추가
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc

  echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu noble stable" > /etc/apt/sources.list.d/docker.list

  # apt 업데이트 및 Docker 설치
  apt-get update -y -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin > /dev/null

  echo "✅ Docker installed successfully: $(docker --version)"
fi

# Docker 데몬 시작 (vfs storage driver 사용)
if ! docker info &> /dev/null; then
  echo "🔄 Starting Docker daemon..."

  # 기존 docker0 인터페이스 정리 (있는 경우)
  if [ -d /sys/class/net/docker0 ]; then
    echo "🧹 Cleaning up existing docker0 interface..."
    ip link delete docker0 2>/dev/null || ifconfig docker0 down 2>/dev/null || true
  fi

  # dockerd 프로세스가 이미 실행 중인지 확인
  if pidof dockerd > /dev/null; then
    echo "⏳ Docker daemon process found, waiting for it to be ready..."
    sleep 3
  else
    dockerd --iptables=false --ip-masq=false --storage-driver=vfs > /var/log/dockerd.log 2>&1 &
  fi

  # Docker 데몬이 준비될 때까지 대기
  for i in {1..15}; do
    if docker info &> /dev/null; then
      echo "✅ Docker daemon started successfully"
      break
    fi
    echo "⏳ Waiting for Docker daemon to start... ($i/15)"
    sleep 2
  done

  if ! docker info &> /dev/null; then
    echo "❌ Failed to start Docker daemon"
    echo "📋 Docker logs:"
    tail -30 /var/log/dockerd.log
    exit 1
  fi
fi

echo "🎉 Docker setup completed successfully!"
docker info 2>&1 | head -5 || true
